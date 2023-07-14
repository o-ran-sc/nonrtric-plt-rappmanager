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
import com.oransc.rappmanager.models.Rapp;
import com.oransc.rappmanager.models.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.RappDeployer;
import com.oransc.rappmanager.models.RappEvent;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.statemachine.RappStateMachine;
import com.oransc.rappmanager.sme.invoker.data.APIInvokerEnrolmentDetails;
import com.oransc.rappmanager.sme.provider.data.APIProviderEnrolmentDetails;
import com.oransc.rappmanager.sme.provider.data.APIProviderFunctionDetails;
import com.oransc.rappmanager.sme.provider.data.ApiProviderFuncRole;
import com.oransc.rappmanager.sme.provider.data.RegistrationInformation;
import com.oransc.rappmanager.sme.publishservice.data.AefProfile;
import com.oransc.rappmanager.sme.publishservice.data.ServiceAPIDescription;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

    private final RappStateMachine rappStateMachine;

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
    public boolean deployRapp(Rapp rapp) {
        logger.debug("Deploying SME functions for Rapp {}", rapp.getName());
        try {
            boolean deployState = createProviderDomain(rapp) && createPublishApi(rapp) && createInvoker(rapp);
            if (deployState) {
                rappStateMachine.sendRappEvent(rapp, RappEvent.SMEDEPLOYED);
            } else {
                rappStateMachine.sendRappEvent(rapp, RappEvent.SMEDEPLOYFAILED);
            }
            return deployState;
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deploy SME functions for Rapp {}", rapp.getName(), e);
        }
        return false;
    }

    @Override
    public boolean undeployRapp(Rapp rapp) {
        logger.debug("Undeploying SME functions for Rapp {}", rapp.getName());
        try {
            rapp.getSmeInvokers().forEach(this::deleteInvoker);
            rapp.getSmeServiceApis().forEach(s -> deletePublishApi(s, rapp.getSmeApfId()));
            rapp.getSmeProviderFunctions().values().forEach(this::deleteProviderFunc);
            rappStateMachine.sendRappEvent(rapp, RappEvent.SMEUNDEPLOYED);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to Undeploy SME functions for Rapp {}", rapp.getName());
        }
        return false;
    }

    boolean createProviderDomain(Rapp rapp) throws JsonProcessingException {
        logger.debug("Creating provider domain for Rapp {}", rapp.getName());
        try {
            String providerDomainPayload = rappCsarConfigurationHandler.getSmeProviderDomainPayload(rapp);
            if (providerDomainPayload != null) {
                APIProviderEnrolmentDetails apiProviderEnrolmentDetails =
                        objectMapper.readValue(providerDomainPayload, APIProviderEnrolmentDetails.class);
                APIProviderEnrolmentDetails responseApiEnrollmentDetails =
                        providerDefaultApiClient.postRegistrations(apiProviderEnrolmentDetails);
                if (responseApiEnrollmentDetails.getApiProvFuncs() != null) {
                    getProviderFuncId(responseApiEnrollmentDetails.getApiProvFuncs(),
                            ApiProviderFuncRole.APF).ifPresent(apiProviderFunctionDetails -> rapp.setSmeApfId(
                            apiProviderFunctionDetails.getApiProvFuncId()));
                    getProviderFuncId(responseApiEnrollmentDetails.getApiProvFuncs(),
                            ApiProviderFuncRole.AEF).ifPresent(apiProviderFunctionDetails -> rapp.setSmeAefId(
                            apiProviderFunctionDetails.getApiProvFuncId()));
                    rapp.setSmeProviderFunctions(responseApiEnrollmentDetails.getApiProvFuncs().stream().collect(
                            Collectors.toMap(APIProviderFunctionDetails::getApiProvFuncInfo,
                                    APIProviderFunctionDetails::getApiProvFuncId)));
                    rappCacheService.putRapp(rapp);
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


    boolean createPublishApi(Rapp rapp) throws JsonProcessingException {
        logger.debug("Creating publish api for Rapp {}", rapp.getName());
        try {
            String providerApiPayload = rappCsarConfigurationHandler.getSmeProviderApiPayload(rapp);
            if (providerApiPayload != null) {
                ServiceAPIDescription serviceAPIDescription =
                        objectMapper.readValue(providerApiPayload, ServiceAPIDescription.class);
                serviceAPIDescription.getAefProfiles().forEach(aefProfile -> {
                    aefProfile.setAefId(rapp.getSmeProviderFunctions().get(aefProfile.getAefId()));
                });
                ServiceAPIDescription serviceAPIDescriptionResponse =
                        publishServiceDefaultApiClient.postApfIdServiceApis(rapp.getSmeApfId(), serviceAPIDescription);

                if (serviceAPIDescriptionResponse.getAefProfiles() != null) {
                    rapp.setSmeServiceApis(
                            serviceAPIDescriptionResponse.getAefProfiles().stream().map(AefProfile::getAefId)
                                    .collect(Collectors.toList()));
                    rappCacheService.putRapp(rapp);
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

    boolean createInvoker(Rapp rapp) throws JsonProcessingException {
        logger.debug("Creating provider domain for Rapp {}", rapp.getName());
        try {
            String invokerPayload = rappCsarConfigurationHandler.getSmeInvokerPayload(rapp);
            if (invokerPayload != null) {
                List<APIInvokerEnrolmentDetails> apiInvokerEnrolmentDetails =
                        objectMapper.readValue(invokerPayload, new TypeReference<>() { });
                apiInvokerEnrolmentDetails.forEach(apiInvokerEnrolmentDetail -> {
                    APIInvokerEnrolmentDetails apiInvokerEnrolmentDetailsResponse =
                            invokerDefaultApiClient.postOnboardedInvokers(apiInvokerEnrolmentDetail);
                    if (apiInvokerEnrolmentDetailsResponse.getApiList() != null) {
                        rapp.getSmeInvokers().addAll(apiInvokerEnrolmentDetailsResponse.getApiList().stream()
                                                             .map(com.oransc.rappmanager.sme.invoker.data.ServiceAPIDescription::getApiId)
                                                             .toList());
                        rappCacheService.putRapp(rapp);
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
