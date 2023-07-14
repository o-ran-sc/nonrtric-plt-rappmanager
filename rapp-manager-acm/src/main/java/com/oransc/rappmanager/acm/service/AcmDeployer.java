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

package com.oransc.rappmanager.acm.service;

import com.oransc.rappmanager.acm.configuration.ACMConfiguration;
import com.oransc.rappmanager.acm.rest.AutomationCompositionDefinitionApiClient;
import com.oransc.rappmanager.acm.rest.AutomationCompositionInstanceApiClient;
import com.oransc.rappmanager.acm.rest.ParticipantMonitoringApiClient;
import com.oransc.rappmanager.models.Rapp;
import com.oransc.rappmanager.models.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.RappDeployer;
import com.oransc.rappmanager.models.RappEvent;
import com.oransc.rappmanager.models.RappState;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.statemachine.RappStateMachine;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class AcmDeployer implements RappDeployer {

    Logger logger = LoggerFactory.getLogger(AcmDeployer.class);

    private final ParticipantMonitoringApiClient participantMonitoringApiClient;
    private final AutomationCompositionDefinitionApiClient automationCompositionDefinitionApiClient;
    private final AutomationCompositionInstanceApiClient automationCompositionInstanceApiClient;
    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;
    private final RappCacheService rappCacheService;
    private final RappStateMachine rappStateMachine;
    private final ACMConfiguration acmConfiguration;
    @Getter
    private UUID compositionId;

    public List<ParticipantInformation> getAllParticipants() {
        return participantMonitoringApiClient.queryParticipants(null, null, UUID.randomUUID());
    }


    void updateACMInstanceState(Rapp rapp, DeployOrder deployOrder) {
        AcInstanceStateUpdate acInstanceStateUpdate = new AcInstanceStateUpdate();
        acInstanceStateUpdate.setDeployOrder(deployOrder);
        automationCompositionInstanceApiClient.compositionInstanceState(rapp.getCompositionId(),
                rapp.getCompositionInstanceId(), acInstanceStateUpdate, UUID.randomUUID());
    }

    public void primeACMComposition(UUID compositionId, PrimeOrder primeOrder) {
        AcTypeStateUpdate acTypeStateUpdate = new AcTypeStateUpdate();
        acTypeStateUpdate.setPrimeOrder(primeOrder);
        automationCompositionDefinitionApiClient.compositionDefinitionPriming(compositionId, UUID.randomUUID(),
                acTypeStateUpdate);
    }

    public CommissioningResponse createComposition(String compositionPayload) {
        CommissioningResponse commissioningResponse = null;
        try {
            commissioningResponse =
                    automationCompositionDefinitionApiClient.createCompositionDefinitions(compositionPayload,
                            UUID.randomUUID());
            compositionId = commissioningResponse.getCompositionId();
        } catch (Exception e) {
            logger.warn("Error in creating composition", e);
        }
        return commissioningResponse;
    }

    public CommissioningResponse deleteComposition(UUID compositionId) {
        return automationCompositionDefinitionApiClient.deleteCompositionDefinition(compositionId, UUID.randomUUID());
    }

    public boolean isCompositionStateEquals(UUID compositionId, AcTypeState acTypeState) {
        //automationCompositionDefinitionApiClient.getCompositionDefinition(compositionId, UUID.randomUUID()).getState().equals(acTypeState);
        //TODO httpmessage converter doesn't map AutomationCompositionDefinition properly, Fix that and check the response
        return true;
    }

    boolean isCompositionInstanceStateEquals(UUID compositionId, UUID compositionIntanceId, DeployState deployState) {
        return automationCompositionInstanceApiClient.getCompositionInstance(compositionId, compositionIntanceId,
                UUID.randomUUID()).getDeployState().equals(deployState);
    }

    boolean waitForCompositionInstanceTargetState(Rapp rapp, DeployState deployState) {
        boolean targetInstanceStateTransition = false;
        try {
            for (int i = 0; i < acmConfiguration.getMaxRetries(); i++) {
                logger.debug("Composition instance state check {}", i + 1);
                if (isCompositionInstanceStateEquals(rapp.getCompositionId(), rapp.getCompositionInstanceId(),
                        deployState)) {
                    sendRappStateEvent(rapp, deployState);
                    logger.info("Composition instance {} state is {}", rapp.getCompositionInstanceId(), deployState);
                    targetInstanceStateTransition = true;
                    break;
                } else {
                    TimeUnit.SECONDS.sleep(acmConfiguration.getRetryInterval());
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to get composition instance state for composition {}", rapp.getCompositionId());
        }
        return targetInstanceStateTransition;
    }

    @Override
    public boolean deployRapp(Rapp rapp) {
        try {
            rapp.setCompositionId(getCompositionId());
            String instantiationPayload =
                    rappCsarConfigurationHandler.getInstantiationPayload(rapp, getCompositionId());
            InstantiationResponse instantiationResponse =
                    automationCompositionInstanceApiClient.createCompositionInstance(getCompositionId(),
                            instantiationPayload, UUID.randomUUID());
            if (instantiationResponse.getInstanceId() != null) {
                rapp.setCompositionInstanceId(instantiationResponse.getInstanceId());
                updateACMInstanceState(rapp, DeployOrder.DEPLOY);
                return true;
            }
        } catch (Exception e) {
            logger.warn("Error in deploying Rapp", e);
        }
        return false;
    }

    @Override
    public boolean undeployRapp(Rapp rapp) {
        AutomationComposition automationComposition =
                automationCompositionInstanceApiClient.getCompositionInstance(rapp.getCompositionId(),
                        rapp.getCompositionInstanceId(), UUID.randomUUID());
        if (automationComposition.getDeployState().equals(DeployState.DEPLOYED) && automationComposition.getLockState()
                                                                                           .equals(LockState.LOCKED)) {
            updateACMInstanceState(rapp, DeployOrder.UNDEPLOY);
            if (waitForCompositionInstanceTargetState(rapp, DeployState.UNDEPLOYED)) {
                automationCompositionInstanceApiClient.deleteCompositionInstance(
                        automationComposition.getCompositionId(), automationComposition.getInstanceId(),
                        UUID.randomUUID());
                rappStateMachine.sendRappEvent(rapp, RappEvent.ACMUNDEPLOYED);
                return true;
            }
        }
        return false;
    }

    public void syncRappStatus(Rapp rapp) {
        if (rapp.getCompositionId() != null && rapp.getCompositionInstanceId() != null) {
            try {
                AutomationComposition compositionInstance =
                        automationCompositionInstanceApiClient.getCompositionInstance(rapp.getCompositionId(),
                                rapp.getCompositionInstanceId(), UUID.randomUUID());
                logger.info("ACM details are " + compositionInstance.toString());
                sendRappStateEvent(rapp, compositionInstance.getDeployState());
            } catch (RestClientException exception) {
                logger.warn("Unable to get the ACM details for rapp {}", rapp.getName());
            }
        }
    }

    void sendRappStateEvent(Rapp rapp, DeployState deployState) {
        if (deployState.equals(DeployState.DEPLOYED)) {
            rappStateMachine.sendRappEvent(rapp, RappEvent.ACMDEPLOYED);
        } else if (deployState.equals(DeployState.UNDEPLOYED)) {
            rappStateMachine.sendRappEvent(rapp, RappEvent.ACMUNDEPLOYED);
        } else if (deployState.equals(DeployState.DEPLOYING)) {
            rappStateMachine.sendRappEvent(rapp, RappEvent.DEPLOYING);
        } else if (deployState.equals(DeployState.UNDEPLOYING)) {
            rappStateMachine.sendRappEvent(rapp, RappEvent.UNDEPLOYING);
        }
    }
}
