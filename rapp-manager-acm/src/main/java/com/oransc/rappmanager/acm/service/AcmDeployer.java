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
import com.oransc.rappmanager.models.RappDeployer;
import com.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappEvent;
import com.oransc.rappmanager.models.rappinstance.RappACMInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
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

    private final AutomationCompositionDefinitionApiClient automationCompositionDefinitionApiClient;
    private final AutomationCompositionInstanceApiClient automationCompositionInstanceApiClient;
    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;
    private final RappInstanceStateMachine rappInstanceStateMachine;
    private final ACMConfiguration acmConfiguration;

    void updateACMInstanceState(UUID compositionId, RappACMInstance rappACMInstance, DeployOrder deployOrder) {
        AcInstanceStateUpdate acInstanceStateUpdate = new AcInstanceStateUpdate();
        acInstanceStateUpdate.setDeployOrder(deployOrder);
        automationCompositionInstanceApiClient.compositionInstanceState(compositionId,
                rappACMInstance.getAcmInstanceId(), acInstanceStateUpdate, UUID.randomUUID());
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
        } catch (Exception e) {
            logger.warn("Error in creating composition", e);
        }
        return commissioningResponse;
    }

    public CommissioningResponse deleteComposition(UUID compositionId) {
        try {
            return automationCompositionDefinitionApiClient.deleteCompositionDefinition(compositionId,
                    UUID.randomUUID());
        } catch (Exception e) {
            logger.warn("Error in deleting composition {}", compositionId, e);
        }
        return null;
    }

    boolean isCompositionInstanceStateEquals(UUID compositionId, UUID compositionIntanceId, DeployState deployState) {
        return automationCompositionInstanceApiClient.getCompositionInstance(compositionId, compositionIntanceId,
                UUID.randomUUID()).getDeployState().equals(deployState);
    }

    boolean waitForCompositionInstanceTargetState(UUID compositionId, RappInstance rappInstance,
            DeployState deployState) {
        boolean targetInstanceStateTransition = false;
        try {
            for (int i = 0; i < acmConfiguration.getMaxRetries(); i++) {
                logger.debug("Composition instance state check {}", i + 1);
                if (isCompositionInstanceStateEquals(compositionId, rappInstance.getAcm().getAcmInstanceId(),
                        deployState)) {
                    sendRappInstanceStateEvent(rappInstance, deployState);
                    logger.info("Composition instance {} state is {}", rappInstance.getAcm().getAcmInstanceId(),
                            deployState);
                    targetInstanceStateTransition = true;
                    break;
                } else {
                    TimeUnit.SECONDS.sleep(acmConfiguration.getRetryInterval());
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to get composition instance state for composition {}", compositionId, e);
            Thread.currentThread().interrupt();
        }
        return targetInstanceStateTransition;
    }

    @Override
    public boolean deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        try {
            String instantiationPayload =
                    rappCsarConfigurationHandler.getInstantiationPayload(rapp, rappInstance.getAcm(),
                            rapp.getCompositionId());
            InstantiationResponse instantiationResponse =
                    automationCompositionInstanceApiClient.createCompositionInstance(rapp.getCompositionId(),
                            instantiationPayload, UUID.randomUUID());
            if (instantiationResponse.getInstanceId() != null) {
                rappInstance.getAcm().setAcmInstanceId(instantiationResponse.getInstanceId());
                updateACMInstanceState(rapp.getCompositionId(), rappInstance.getAcm(), DeployOrder.DEPLOY);
                return true;
            }
        } catch (Exception e) {
            logger.warn("Error in deploying Rapp", e);
        }
        rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.ACMDEPLOYFAILED);
        rappInstance.setReason("Unable to deploy ACM");
        return false;
    }

    @Override
    public boolean undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        AutomationComposition automationComposition =
                automationCompositionInstanceApiClient.getCompositionInstance(rapp.getCompositionId(),
                        rappInstance.getAcm().getAcmInstanceId(), UUID.randomUUID());
        if (automationComposition.getDeployState().equals(DeployState.DEPLOYED) && automationComposition.getLockState()
                                                                                           .equals(LockState.LOCKED)) {
            updateACMInstanceState(rapp.getCompositionId(), rappInstance.getAcm(), DeployOrder.UNDEPLOY);
            if (waitForCompositionInstanceTargetState(rapp.getCompositionId(), rappInstance, DeployState.UNDEPLOYED)) {
                automationCompositionInstanceApiClient.deleteCompositionInstance(
                        automationComposition.getCompositionId(), automationComposition.getInstanceId(),
                        UUID.randomUUID());
                rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.ACMUNDEPLOYED);
                return true;
            }
        }
        rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.ACMUNDEPLOYFAILED);
        rappInstance.setReason("Unable to undeploy ACM");
        return false;
    }

    @Override
    public boolean primeRapp(Rapp rapp) {
        logger.info("Priming rapp {}", rapp.getName());
        try {
            String compositionPayload = rappCsarConfigurationHandler.getAcmCompositionPayload(rapp);
            CommissioningResponse commissioningResponse = createComposition(compositionPayload);
            if (commissioningResponse != null && commissioningResponse.getCompositionId() != null) {
                rapp.setCompositionId(commissioningResponse.getCompositionId());
                logger.info("Priming automation Composition");
                primeACMComposition(commissioningResponse.getCompositionId(), PrimeOrder.PRIME);
                return true;
            } else {
                logger.warn("Failed to create automation composition");
            }
        } catch (Exception e) {
            logger.warn("Error in creating automation composition", e);
        }
        rapp.setReason("Unable to create automation composition");
        return false;
    }

    @Override
    public boolean deprimeRapp(Rapp rapp) {
        try {
            primeACMComposition(rapp.getCompositionId(), PrimeOrder.DEPRIME);
            if (waitForCompositionTargetState(rapp.getCompositionId(), AcTypeState.COMMISSIONED)) {
                CommissioningResponse commissioningResponse = deleteComposition(rapp.getCompositionId());
                if (commissioningResponse != null) {
                    rapp.setCompositionId(null);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed deprime automation composition", e);
        }
        rapp.setReason("Unable to delete automation composition");
        return false;
    }

    boolean waitForCompositionTargetState(UUID compositionId, AcTypeState acTypeState) {
        boolean targetCompositionStateTransition = false;
        try {
            for (int i = 0; i < acmConfiguration.getMaxRetries(); i++) {
                logger.debug("Composition state check {}", i + 1);
                if (isCompositionStateEquals(compositionId, acTypeState)) {
                    logger.debug("Composition {} state is {}", compositionId, acTypeState);
                    targetCompositionStateTransition = true;
                    break;
                } else {
                    TimeUnit.SECONDS.sleep(acmConfiguration.getRetryInterval());
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to get composition state for composition {}", compositionId, e);
            Thread.currentThread().interrupt();
        }
        return targetCompositionStateTransition;
    }

    boolean isCompositionStateEquals(UUID compositionId, AcTypeState acTypeState) {
        return automationCompositionDefinitionApiClient.getCompositionDefinition(compositionId, UUID.randomUUID())
                       .getState().equals(acTypeState);
    }

    public void syncRappInstanceStatus(UUID compositionId, RappInstance rappInstance) {
        if (rappInstance.getAcm().getAcmInstanceId() != null) {
            try {
                AutomationComposition compositionInstance =
                        automationCompositionInstanceApiClient.getCompositionInstance(compositionId,
                                rappInstance.getAcm().getAcmInstanceId(), UUID.randomUUID());
                sendRappInstanceStateEvent(rappInstance, compositionInstance.getDeployState());
            } catch (RestClientException exception) {
                logger.warn("Unable to get the ACM details for rapp instance {}", rappInstance.getRappInstanceId());
            }
        }
    }

    void sendRappInstanceStateEvent(RappInstance rappInstance, DeployState deployState) {
        if (deployState.equals(DeployState.DEPLOYED)) {
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.ACMDEPLOYED);
        } else if (deployState.equals(DeployState.UNDEPLOYED)) {
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.ACMUNDEPLOYED);
        } else if (deployState.equals(DeployState.DEPLOYING)) {
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.DEPLOYING);
        } else if (deployState.equals(DeployState.UNDEPLOYING)) {
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.UNDEPLOYING);
        }
    }
}
