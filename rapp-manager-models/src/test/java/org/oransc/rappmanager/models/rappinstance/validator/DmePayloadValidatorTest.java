/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
 *
 */

package org.oransc.rappmanager.models.rappinstance.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappResources;
import org.oransc.rappmanager.models.rappinstance.RappDMEInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {DmePayloadValidator.class})
class DmePayloadValidatorTest {

    private static final String PRODUCER_INFOTYPE_1 = "producer-infotype-1";
    private static final String PRODUCER_INFOTYPE_2 = "producer-infotype-2";
    private static final String CONSUMER_INFOTYPE_1 = "consumer-infotype-1";
    private static final String CONSUMER_INFOTYPE_2 = "consumer-infotype-2";
    private static final String INFO_CONSUMER_1 = "info-consumer-1";
    private static final String INFO_CONSUMER_2 = "info-consumer-2";
    private static final String INFO_PRODUCER_1 = "info-producer-1";
    private static final String INFO_PRODUCER_2 = "info-producer-2";

    @Autowired
    private DmePayloadValidator dmePayloadValidator;

    @Test
    void testDmePayloadValidatorSuccess() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of(PRODUCER_INFOTYPE_1));
        rappDMEInstance.setInfoTypeConsumer(CONSUMER_INFOTYPE_1);
        rappDMEInstance.setInfoConsumer(INFO_CONSUMER_1);
        rappDMEInstance.setInfoProducer(INFO_PRODUCER_1);
        rappInstance.setDme(rappDMEInstance);
        assertDoesNotThrow(() -> dmePayloadValidator.validate(rapp, rappInstance));
    }

    @Test
    void testDmePayloadValidatorSuccessWithNull() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(null);
        rappDMEInstance.setInfoTypeConsumer(null);
        rappDMEInstance.setInfoConsumer(null);
        rappDMEInstance.setInfoProducer(INFO_PRODUCER_1);
        rappInstance.setDme(rappDMEInstance);
        assertDoesNotThrow(() -> dmePayloadValidator.validate(rapp, rappInstance));
    }

    @Test
    void testDmePayloadValidatorFailureInvalidProducerInfoType() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of("invalid-infotype"));
        rappDMEInstance.setInfoTypeConsumer(CONSUMER_INFOTYPE_1);
        rappDMEInstance.setInfoConsumer(INFO_CONSUMER_1);
        rappDMEInstance.setInfoProducer(INFO_PRODUCER_1);
        rappInstance.setDme(rappDMEInstance);

        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> dmePayloadValidator.validate(rapp, rappInstance));
        assertEquals("Invalid DME info types producer in the rApp instance payload.",
                rappValidationException.getMessage());
    }

    @Test
    void testDmePayloadValidatorFailureInvalidConsumerInfoType() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of(PRODUCER_INFOTYPE_1));
        rappDMEInstance.setInfoTypeConsumer("invalid-infotype");
        rappDMEInstance.setInfoConsumer(INFO_CONSUMER_1);
        rappDMEInstance.setInfoProducer(INFO_PRODUCER_1);
        rappInstance.setDme(rappDMEInstance);

        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> dmePayloadValidator.validate(rapp, rappInstance));
        assertEquals("Invalid DME info type consumer in the rApp instance payload.",
                rappValidationException.getMessage());
    }

    @Test
    void testDmePayloadValidatorFailureInvalidInfoConsumer() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of(PRODUCER_INFOTYPE_1));
        rappDMEInstance.setInfoTypeConsumer(CONSUMER_INFOTYPE_1);
        rappDMEInstance.setInfoConsumer("invalid-consumer");
        rappDMEInstance.setInfoProducer(INFO_PRODUCER_1);
        rappInstance.setDme(rappDMEInstance);

        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> dmePayloadValidator.validate(rapp, rappInstance));
        assertEquals("Invalid DME info consumer in the rApp instance payload.", rappValidationException.getMessage());
    }

    @Test
    void testDmePayloadValidatorFailureInvalidInfoProducer() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of(PRODUCER_INFOTYPE_1));
        rappDMEInstance.setInfoTypeConsumer(CONSUMER_INFOTYPE_1);
        rappDMEInstance.setInfoConsumer(INFO_CONSUMER_1);
        rappDMEInstance.setInfoProducer("invalid-producer");
        rappInstance.setDme(rappDMEInstance);

        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> dmePayloadValidator.validate(rapp, rappInstance));
        assertEquals("Invalid DME info producer in the rApp instance payload.", rappValidationException.getMessage());
    }


    private Rapp getRapp() {
        RappResources rappResources = new RappResources();
        RappResources.DMEResources rappDMEResources =
                RappResources.DMEResources.builder().producerInfoTypes(Set.of(PRODUCER_INFOTYPE_1, PRODUCER_INFOTYPE_2))
                        .consumerInfoTypes(Set.of(CONSUMER_INFOTYPE_1, CONSUMER_INFOTYPE_2))
                        .infoConsumers(Set.of(INFO_CONSUMER_1, INFO_CONSUMER_2))
                        .infoProducers(Set.of(INFO_PRODUCER_1, INFO_PRODUCER_2)).build();

        rappResources.setDme(rappDMEResources);
        return Rapp.builder().rappResources(rappResources).build();
    }
}
