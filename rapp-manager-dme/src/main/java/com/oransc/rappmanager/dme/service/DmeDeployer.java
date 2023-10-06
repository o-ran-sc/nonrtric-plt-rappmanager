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

package com.oransc.rappmanager.dme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.dme.data.ConsumerJob;
import com.oransc.rappmanager.dme.data.ProducerInfoTypeInfo;
import com.oransc.rappmanager.dme.data.ProducerRegistrationInfo;
import com.oransc.rappmanager.dme.rest.DataConsumerApiClient;
import com.oransc.rappmanager.dme.rest.DataProducerRegistrationApiClient;
import com.oransc.rappmanager.models.RappDeployer;
import com.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappEvent;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DmeDeployer implements RappDeployer {

    Logger logger = LoggerFactory.getLogger(DmeDeployer.class);

    private final DataProducerRegistrationApiClient dataProducerRegistrationApiClient;

    private final DataConsumerApiClient dataConsumerApiClient;

    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;

    private final ObjectMapper objectMapper;

    private final RappInstanceStateMachine rappInstanceStateMachine;

    @Override
    public boolean deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        logger.debug("Deploying DME functions for RappInstance {}", rappInstance.getRappInstanceId());
        boolean deployState = true;
        if (rappInstance.getDme().getInfoTypesProducer() != null) {
            deployState = createProducerInfoTypes(rapp, rappInstance.getDme().getInfoTypesProducer());
        }
        if (rappInstance.getDme().getInfoTypeConsumer() != null) {
            deployState =
                    deployState && createConsumerInfoTypes(rapp, Set.of(rappInstance.getDme().getInfoTypeConsumer()));
        }
        if (rappInstance.getDme().getInfoProducer() != null) {
            deployState = deployState && createInfoProducer(rapp, rappInstance.getDme().getInfoProducer());
        }
        if (rappInstance.getDme().getInfoConsumer() != null) {
            deployState = deployState && createInfoConsumer(rapp, rappInstance.getDme().getInfoConsumer());
        }
        if (deployState) {
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.DMEDEPLOYED);
        } else {
            rappInstance.setReason("Unable to deploy DME");
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.DMEDEPLOYFAILED);
        }
        return deployState;
    }

    @Override
    public boolean undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        logger.debug("Undeploying DME functions for RappInstance {}", rappInstance.getRappInstanceId());
        boolean undeployState = true;
        if (rappInstance.getDme().getInfoConsumer() != null) {
            undeployState = deleteInfoConsumer(rapp, rappInstance.getDme().getInfoConsumer());
        }
        if (rappInstance.getDme().getInfoProducer() != null) {
            undeployState = undeployState && deleteInfoProducer(rapp, rappInstance.getDme().getInfoProducer());
        }
        if (undeployState) {
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.DMEUNDEPLOYED);
        } else {
            rappInstance.setReason("Unable to undeploy DME");
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.DMEUNDEPLOYFAILED);
        }
        return undeployState;
    }

    @Override
    public boolean primeRapp(Rapp rapp) {
        logger.debug("Priming DME functions for rApp {}", rapp.getRappId());
        try {
            Set<String> requiredInfoTypes = new HashSet<>();
            for (String producerResourceName : rapp.getRappResources().getDme().getInfoProducers()) {
                String producerPayload =
                        rappCsarConfigurationHandler.getDmeInfoProducerPayload(rapp, producerResourceName);
                ProducerRegistrationInfo producerRegistrationInfo =
                        objectMapper.readValue(producerPayload, ProducerRegistrationInfo.class);
                requiredInfoTypes.addAll(producerRegistrationInfo.getSupportedInfoTypes());
            }
            for (String consumerResourceName : rapp.getRappResources().getDme().getInfoConsumers()) {
                String consumerPayload =
                        rappCsarConfigurationHandler.getDmeInfoConsumerPayload(rapp, consumerResourceName);
                ConsumerJob consumerJob = objectMapper.readValue(consumerPayload, ConsumerJob.class);
                requiredInfoTypes.add(consumerJob.getInfoTypeId());
            }
            Set<String> allInfoTypes = new HashSet<>(rapp.getRappResources().getDme().getProducerInfoTypes());
            allInfoTypes.addAll(rapp.getRappResources().getDme().getConsumerInfoTypes());
            requiredInfoTypes.removeAll(allInfoTypes);
            if (!requiredInfoTypes.isEmpty()) {
                allInfoTypes.addAll(dataProducerRegistrationApiClient.getInfoTypdentifiers());
                requiredInfoTypes.removeAll(allInfoTypes);
                if (!requiredInfoTypes.isEmpty()) {
                    rapp.setReason(String.format("Invalid rapp package as the following info types cannot be found %s",
                            requiredInfoTypes));
                }
            }
            return true;
        } catch (Exception e) {
            logger.warn("Failed to prime DME", e);
            rapp.setReason("Failed to prime DME");
            return false;
        }
    }

    @Override
    public boolean deprimeRapp(Rapp rapp) {
        logger.debug("Depriming DME functions for rApp {}", rapp.getRappId());
        return true;
    }

    boolean createProducerInfoTypes(Rapp rApp, Set<String> infoTypes) {
        logger.debug("Creating DME producer info types {} for rApp {}", infoTypes, rApp.getRappId());
        return createInfoTypes(rApp, infoTypes, rappCsarConfigurationHandler::getDmeProducerInfoTypePayload);
    }

    boolean createConsumerInfoTypes(Rapp rApp, Set<String> infoTypes) {
        logger.debug("Creating DME consumer info types {} for rApp {}", infoTypes, rApp.getRappId());
        return createInfoTypes(rApp, infoTypes, rappCsarConfigurationHandler::getDmeConsumerInfoTypePayload);
    }

    boolean createInfoTypes(Rapp rApp, Set<String> infoTypes, BiFunction<Rapp, String, String> payloadReader) {
        try {
            Map<String, ProducerInfoTypeInfo> producerInfoTypeInfoMap = new HashMap<>();
            for (String infoType : infoTypes) {
                String infoTypePayload = payloadReader.apply(rApp, infoType);
                if (infoTypePayload != null && !infoTypePayload.isEmpty()) {
                    producerInfoTypeInfoMap.put(infoType,
                            objectMapper.readValue(infoTypePayload, ProducerInfoTypeInfo.class));
                }
            }
            return producerInfoTypeInfoMap.entrySet().stream().map(stringProducerInfoTypeInfoEntry -> {
                ResponseEntity<Object> objectResponseEntity = dataProducerRegistrationApiClient.putInfoTypeWithHttpInfo(
                        stringProducerInfoTypeInfoEntry.getKey(), stringProducerInfoTypeInfoEntry.getValue());
                return objectResponseEntity.getStatusCode().is2xxSuccessful();
            }).reduce(true, (a, b) -> a && b);
        } catch (Exception e) {
            logger.warn("Error in creating info types {} for rApp {}", infoTypes, rApp.getRappId(), e);
            return false;
        }
    }

    boolean createInfoProducer(Rapp rApp, String producerResource) {
        logger.debug("Creating DME info producer {} for rApp {}", producerResource, rApp.getRappId());
        try {
            String infoProducerPayload = rappCsarConfigurationHandler.getDmeInfoProducerPayload(rApp, producerResource);
            ProducerRegistrationInfo producerRegistrationInfo =
                    objectMapper.readValue(infoProducerPayload, ProducerRegistrationInfo.class);

            ResponseEntity<Object> objectResponseEntity =
                    dataProducerRegistrationApiClient.putInfoProducerWithHttpInfo(producerResource,
                            producerRegistrationInfo);
            return objectResponseEntity.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Error in creating info producer {} for rApp {}", producerResource, rApp.getRappId(), e);
            return false;
        }
    }

    boolean createInfoConsumer(Rapp rApp, String consumerResource) {
        logger.debug("Creating DME info consumer {} for rApp {}", consumerResource, rApp.getRappId());
        try {
            String infoJobPayload = rappCsarConfigurationHandler.getDmeInfoConsumerPayload(rApp, consumerResource);
            ConsumerJob consumerJob = objectMapper.readValue(infoJobPayload, ConsumerJob.class);
            ResponseEntity<Object> objectResponseEntity =
                    dataConsumerApiClient.putIndividualInfoJobWithHttpInfo(consumerResource, consumerJob);
            return objectResponseEntity.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Error in creating info consumer {} for rApp {}", consumerResource, rApp.getRappId(), e);
            return false;
        }
    }

    boolean deleteInfoProducer(Rapp rApp, String producerResource) {
        logger.debug("Deleting DME info producer {} for rApp {}", producerResource, rApp.getRappId());
        try {
            ResponseEntity<Object> objectResponseEntity =
                    dataProducerRegistrationApiClient.deleteInfoProducerWithHttpInfo(producerResource);
            return objectResponseEntity.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Error in deleting info producer {} for rApp {}", producerResource, rApp.getRappId(), e);
            return false;
        }
    }

    boolean deleteInfoConsumer(Rapp rApp, String consumerResource) {
        logger.debug("Deleting DME info consumer {} for rApp {}", consumerResource, rApp.getRappId());
        try {
            ResponseEntity<Object> objectResponseEntity =
                    dataConsumerApiClient.deleteIndividualInfoJobWithHttpInfo(consumerResource);
            return objectResponseEntity.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Error in deleting info consumer {} for rApp {}", consumerResource, rApp.getRappId(), e);
            return false;
        }
    }
}
