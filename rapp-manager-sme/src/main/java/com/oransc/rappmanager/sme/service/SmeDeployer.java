/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * ===============================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END========================================================================
 */

package com.oransc.rappmanager.sme.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.RappDeployer;
import com.oransc.rappmanager.models.rapp.RappEvent;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import com.oransc.rappmanager.sme.invoker.data.APIInvokerEnrolmentDetails;
import com.oransc.rappmanager.sme.provider.data.APIProviderEnrolmentDetails;
import com.oransc.rappmanager.sme.provider.data.APIProviderFunctionDetails;
import com.oransc.rappmanager.sme.provider.data.ApiProviderFuncRole;
import com.oransc.rappmanager.sme.provider.data.RegistrationInformation;
import com.oransc.rappmanager.sme.publishservice.data.ServiceAPIDescription;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmeDeployer implements RappDeployer {

    Logger logger = LoggerFactory.getLogger(SmeDeployer.class);

    private final com.oransc.rappmanager.sme.provider.rest.DefaultApiClient providerDefaultApiClient;

    private final com.oransc.rappmanager.sme.publishservice.rest.DefaultApiClient publishServiceDefaultApiClient;

    private final com.oransc.rappmanager.sme.invoker.rest.DefaultApiClient invokerDefaultApiClient;

    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;

    private final ObjectMapper objectMapper;

    private final RappCacheService rappCacheService;

    private final RappInstanceStateMachine rappInstanceStateMachine;

    private String amfRegistrationId;


    APIProviderEnrolmentDetails createAMF() {
        APIProviderEnrolmentDetails responseApiEnrollmentDetails = null;
        try {
            APIProviderFunctionDetails apiProviderFunctionDetails = new APIProviderFunctionDetails();
            apiProviderFunctionDetails.setApiProvFuncRole(ApiProviderFuncRole.AMF);
            apiProviderFunctionDetails.setApiProvFuncInfo("Rapp Manager as AMF");
            apiProviderFunctionDetails.setRegInfo(new RegistrationInformation().apiProvPubKey("asd"));

            APIProviderEnrolmentDetails apiProviderEnrolmentDetails = new APIProviderEnrolmentDetails();
            apiProviderEnrolmentDetails.setRegSec("PSK");
            apiProviderEnrolmentDetails.setApiProvDomInfo("Rapp Manager as AMF");
            apiProviderEnrolmentDetails.setApiProvFuncs(List.of(apiProviderFunctionDetails));
            responseApiEnrollmentDetails = providerDefaultApiClient.postRegistrations(apiProviderEnrolmentDetails);
            amfRegistrationId = responseApiEnrollmentDetails.getApiProvDomId();
        } catch (Exception e) {
            logger.warn("Error in creating AMF", e);
        }
        return responseApiEnrollmentDetails;
    }

    void deleteAMF() {
        deleteProviderFunc(amfRegistrationId);
    }

    @Override
    public boolean deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        logger.debug("Deploying SME functions for RappInstance {}", rappInstance.getRappInstanceId());
        try {
            boolean deployState =
                    createProviderDomain(rapp, rappInstance) && createPublishApi(rapp, rappInstance) && createInvoker(
                            rapp, rappInstance);
            if (deployState) {
                rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.SMEDEPLOYED);
            } else {
                rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.SMEDEPLOYFAILED);
            }
            return deployState;
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deploy SME functions for Rapp {}", rapp.getName(), e);
        }
        return false;
    }

    @Override
    public boolean undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        logger.debug("Undeploying SME functions for Rapp {}", rapp.getName());
        try {
            rappInstance.getSme().getInvokerIds().forEach(this::deleteInvoker);
            rappInstance.getSme().getServiceApiIds()
                    .forEach(s -> deletePublishApi(s, rappInstance.getSme().getApfId()));
            rappInstance.getSme().getProviderFunctionIds().forEach(this::deleteProviderFunc);
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.SMEUNDEPLOYED);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to Undeploy SME functions for Rapp {}", rapp.getName(), e);
        }
        return false;
    }

    @Override
    public boolean primeRapp(Rapp rapp) {
        //If there is any priming operations on SME
        return true;
    }

    @Override
    public boolean deprimeRapp(Rapp rapp) {
        //If there is any deprimgng operations
        return true;
    }

    boolean createProviderDomain(Rapp rapp, RappInstance rappInstance) throws JsonProcessingException {
        logger.debug("Creating provider domain for Rapp {}", rapp.getName());
        try {
            String providerDomainPayload =
                    rappCsarConfigurationHandler.getSmeProviderDomainPayload(rapp, rappInstance.getSme());
            logger.info("provider domain payload " + providerDomainPayload);
            if (providerDomainPayload != null) {
                APIProviderEnrolmentDetails apiProviderEnrolmentDetails =
                        objectMapper.readValue(providerDomainPayload, APIProviderEnrolmentDetails.class);
                //TODO Fix this Temporary workaround
                apiProviderEnrolmentDetails.setRegSec(
                        apiProviderEnrolmentDetails.getRegSec() + rappInstance.getRappInstanceId());
                APIProviderEnrolmentDetails responseApiEnrollmentDetails =
                        providerDefaultApiClient.postRegistrations(apiProviderEnrolmentDetails);
                if (responseApiEnrollmentDetails.getApiProvFuncs() != null) {
                    rappInstance.getSme().setProviderFunctionIds(responseApiEnrollmentDetails.getApiProvFuncs().stream()
                                                                         .map(APIProviderFunctionDetails::getApiProvFuncId)
                                                                         .toList());

                    getProviderFuncId(responseApiEnrollmentDetails.getApiProvFuncs(),
                            ApiProviderFuncRole.APF).ifPresent(apiProviderFunctionDetails -> rappInstance.getSme()
                                                                                                     .setApfId(
                                                                                                             apiProviderFunctionDetails.getApiProvFuncId()));
                    getProviderFuncId(responseApiEnrollmentDetails.getApiProvFuncs(),
                            ApiProviderFuncRole.AEF).ifPresent(apiProviderFunctionDetails -> rappInstance.getSme()
                                                                                                     .setAefId(
                                                                                                             apiProviderFunctionDetails.getApiProvFuncId()));
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Error in creating provider domain", e);
        }
        return false;
    }

    Optional<APIProviderFunctionDetails> getProviderFuncId(List<APIProviderFunctionDetails> apiProviderFunctionDetails,
            ApiProviderFuncRole apiProviderFuncRole) {
        return apiProviderFunctionDetails.stream()
                       .filter(apiProviderFunctionDetail -> apiProviderFunctionDetail.getApiProvFuncRole()
                                                                    .equals(apiProviderFuncRole)).findFirst();
    }

    void deleteProviderFunc(String registrationId) {
        providerDefaultApiClient.deleteRegistrationsRegistrationId(registrationId);
    }


    boolean createPublishApi(Rapp rapp, RappInstance rappInstance) throws JsonProcessingException {
        logger.debug("Creating publish api for Rapp {}", rapp.getName());
        try {
            String providerApiPayload =
                    rappCsarConfigurationHandler.getSmeProviderApiPayload(rapp, rappInstance.getSme());
            if (providerApiPayload != null) {
                ServiceAPIDescription serviceAPIDescription =
                        objectMapper.readValue(providerApiPayload, ServiceAPIDescription.class);
                serviceAPIDescription.getAefProfiles().forEach(aefProfile -> {
                    aefProfile.setAefId(rappInstance.getSme().getAefId());
                });
                ServiceAPIDescription serviceAPIDescriptionResponse =
                        publishServiceDefaultApiClient.postApfIdServiceApis(rappInstance.getSme().getApfId(),
                                serviceAPIDescription);

                if (serviceAPIDescriptionResponse.getAefProfiles() != null) {
                    rappInstance.getSme().setServiceApiIds(List.of(serviceAPIDescriptionResponse.getApiId()));
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Error in creating publish api", e);
        }
        return false;
    }

    void deletePublishApi(String serviceApiId, String apfId) {
        publishServiceDefaultApiClient.deleteApfIdServiceApisServiceApiId(serviceApiId, apfId);
    }

    boolean createInvoker(Rapp rapp, RappInstance rappInstance) throws JsonProcessingException {
        logger.debug("Creating provider domain for Rapp {}", rapp.getName());
        try {
            String invokerPayload = rappCsarConfigurationHandler.getSmeInvokerPayload(rapp, rappInstance.getSme());
            if (invokerPayload != null) {
                List<APIInvokerEnrolmentDetails> apiInvokerEnrolmentDetails =
                        objectMapper.readValue(invokerPayload, new TypeReference<>() { });
                apiInvokerEnrolmentDetails.forEach(apiInvokerEnrolmentDetail -> {
                    APIInvokerEnrolmentDetails apiInvokerEnrolmentDetailsResponse =
                            invokerDefaultApiClient.postOnboardedInvokers(apiInvokerEnrolmentDetail);
                    if (apiInvokerEnrolmentDetailsResponse.getApiList() != null) {
                        rappInstance.getSme()
                                .setInvokerIds(List.of(apiInvokerEnrolmentDetailsResponse.getApiInvokerId()));
                    }
                });
                return true;
            }
        } catch (Exception e) {
            logger.warn("Error in creating invoker", e);
        }
        return false;
    }

    void deleteInvoker(String invokerId) {
        invokerDefaultApiClient.deleteOnboardedInvokersOnboardingId(invokerId);
    }
}
