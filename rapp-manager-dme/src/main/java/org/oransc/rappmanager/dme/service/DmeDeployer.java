/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.dme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.dme.data.ConsumerJob;
import org.oransc.rappmanager.dme.data.ProducerRegistrationInfo;
import org.oransc.rappmanager.dme.rest.DataProducerRegistrationApiClient;
import org.oransc.rappmanager.models.RappDeployer;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DmeDeployer implements RappDeployer {

    Logger logger = LoggerFactory.getLogger(DmeDeployer.class);

    private final DataProducerRegistrationApiClient dataProducerRegistrationApiClient;

    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;

    private final ObjectMapper objectMapper;

    @Override
    public boolean deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        logger.debug("DME instance deployment is handled as part of ACM injection for {}",
                rappInstance.getRappInstanceId());
        return true;
    }

    @Override
    public boolean undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        logger.debug("DME instance undeployment is handled as part of ACM injection for {}",
                rappInstance.getRappInstanceId());
        return true;
    }

    @Override
    public boolean primeRapp(Rapp rapp) {
        logger.debug("Priming DME functions for rApp {}", rapp.getRappId());
        if (rapp.isDMEEnabled()) {
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
                        rapp.setReason(
                                String.format("Invalid rapp package as the following info types cannot be found %s",
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
        return true;
    }

    @Override
    public boolean deprimeRapp(Rapp rapp) {
        logger.debug("Depriming DME functions for rApp {}", rapp.getRappId());
        return true;
    }
}
