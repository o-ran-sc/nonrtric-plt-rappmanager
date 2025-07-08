/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.oransc.participant.dme.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.hc.core5.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV1;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.oransc.participant.dme.exception.DmeException;
import org.oransc.participant.dme.models.ConfigurationEntity;
import org.oransc.participant.dme.models.DataConsumerEntity;
import org.oransc.participant.dme.models.DataProducerEntity;
import org.oransc.participant.dme.models.InfoTypeEntity;
import org.oransc.participant.dme.restclient.AcDmeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AutomationCompositionElementHandler extends AcElementListenerV1 {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AcDmeClient acDmeClient;

    // Map of acElement Id and DME services
    @Getter(AccessLevel.PACKAGE)
    private final Map<UUID, ConfigurationEntity> configRequestMap = new ConcurrentHashMap<>();

    public AutomationCompositionElementHandler(ParticipantIntermediaryApi intermediaryApi, AcDmeClient acDmeClient) {
        super(intermediaryApi);
        this.acDmeClient = acDmeClient;
    }

    @Override
    public void undeploy(UUID automationCompositionId, UUID automationCompositionElementId) throws DmeException {
        var configurationEntity = configRequestMap.get(automationCompositionElementId);
        if (configurationEntity != null && acDmeClient.isDmeHealthy()) {
            if (configurationEntity.getDataConsumerEntities() != null) {
                acDmeClient.deleteDataConsumer(configurationEntity.getDataConsumerEntities().stream()
                                                       .map(DataConsumerEntity::getDataConsumerId)
                                                       .collect(Collectors.toSet()));
            }
            if (configurationEntity.getDataProducerEntities() != null) {
                acDmeClient.deleteDataProducer(configurationEntity.getDataProducerEntities().stream()
                                                       .map(DataProducerEntity::getDataProducerId)
                                                       .collect(Collectors.toSet()));
            }
            configRequestMap.remove(automationCompositionElementId);
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                    "Undeployed");
        } else {
            LOGGER.warn("Failed to connect with DME. Service configuration is: {}", configurationEntity);
            throw new DmeException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Unable to connect with DME");
        }
    }

    @Override
    public void deploy(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws DmeException {
        try {
            var configurationEntity = CODER.convert(properties, ConfigurationEntity.class);
            var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(configurationEntity);
            if (violations.isEmpty()) {
                if (acDmeClient.isDmeHealthy()) {
                    if (configurationEntity.getInfoTypeEntities() != null) {
                        acDmeClient.createInfoType(configurationEntity.getInfoTypeEntities().stream().collect(
                                Collectors.toMap(InfoTypeEntity::getInfoTypeId, InfoTypeEntity::getPayload)));
                    }
                    if (configurationEntity.getDataProducerEntities() != null) {
                        acDmeClient.createDataProducer(configurationEntity.getDataProducerEntities().stream().collect(
                                Collectors.toMap(DataProducerEntity::getDataProducerId,
                                        DataProducerEntity::getPayload)));
                    }
                    if (configurationEntity.getDataConsumerEntities() != null) {
                        acDmeClient.createDataConsumer(configurationEntity.getDataConsumerEntities().stream().collect(
                                Collectors.toMap(DataConsumerEntity::getDataConsumerId,
                                        DataConsumerEntity::getPayload)));
                    }

                    configRequestMap.put(element.getId(), configurationEntity);
                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                            DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
                } else {
                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                            DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Unable to connect with DME ");
                    throw new DmeException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Unable to connect with DME");
                }
            } else {
                LOGGER.error("Violations found in the config request parameters: {}", violations);
                throw new ValidationException("Constraint violations in the config request");
            }
        } catch (JsonProcessingException | ValidationException | CoderException | DmeException e) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, e.getMessage());
            throw new DmeException(HttpStatus.SC_BAD_REQUEST, "Invalid Configuration", e);
        }
    }

    @Override
    public void lock(UUID instanceId, UUID elementId) {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.NO_ERROR, "Locked");
    }

    @Override
    public void unlock(UUID instanceId, UUID elementId) {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Override
    public void delete(UUID instanceId, UUID elementId) {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, DeployState.DELETED, null,
                StateChangeResult.NO_ERROR, "Deleted");
    }

    @Override
    public void update(UUID instanceId, AcElementDeploy element, Map<String, Object> properties) {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Override
    public void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList) {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
    }

    @Override
    public void deprime(UUID compositionId) {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
                "Deprimed");
    }

    @Override
    public void handleRestartComposition(UUID compositionId,
            List<AutomationCompositionElementDefinition> elementDefinitionList, AcTypeState state) {
        var finalState = AcTypeState.PRIMED.equals(state) || AcTypeState.PRIMING.equals(state) ? AcTypeState.PRIMED
                                 : AcTypeState.COMMISSIONED;
        intermediaryApi.updateCompositionState(compositionId, finalState, StateChangeResult.NO_ERROR, "Restarted");
    }

    @Override
    public void handleRestartInstance(UUID automationCompositionId, AcElementDeploy element,
            Map<String, Object> properties, DeployState deployState, LockState lockState) throws PfModelException {
        if (DeployState.DEPLOYING.equals(deployState)) {
            deploy(automationCompositionId, element, properties);
            return;
        }
        if (DeployState.UNDEPLOYING.equals(deployState) || DeployState.DEPLOYED.equals(deployState)
                    || DeployState.UPDATING.equals(deployState)) {
            try {
                var configurationEntity = CODER.convert(properties, ConfigurationEntity.class);
                configRequestMap.put(element.getId(), configurationEntity);
            } catch (ValidationException | CoderException e) {
                throw new DmeException(HttpStatus.SC_BAD_REQUEST, "Invalid Configuration", e);
            }
        }
        if (DeployState.UNDEPLOYING.equals(deployState)) {
            undeploy(automationCompositionId, element.getId());
            return;
        }
        deployState = AcmUtils.deployCompleted(deployState);
        lockState = AcmUtils.lockCompleted(deployState, lockState);
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(), deployState,
                lockState, StateChangeResult.NO_ERROR, "Restarted");
    }

    @Override
    public void migrate(UUID automationCompositionId, AcElementDeploy element, UUID compositionTargetId,
            Map<String, Object> properties) {
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
