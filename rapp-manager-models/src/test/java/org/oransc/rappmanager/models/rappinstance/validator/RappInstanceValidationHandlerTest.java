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
import org.oransc.rappmanager.models.rapp.RappState;
import org.oransc.rappmanager.models.rappinstance.RappACMInstance;
import org.oransc.rappmanager.models.rappinstance.RappDMEInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(
        classes = {RappInstanceValidationHandler.class, RappStatusValidator.class, AcmPayloadValidator.class,
                SmePayloadValidator.class, DmePayloadValidator.class})
class RappInstanceValidationHandlerTest {

    public static final String K8S_INSTANCE = "k8s-instance";
    public static final String A1PMS_INSTANCE = "a1pms-instance";
    public static final String PROVIDER_1 = "provider-1";
    public static final String PROVIDER_2 = "provider-2";
    public static final String INV_1 = "inv-1";
    public static final String INV_2 = "inv-2";
    public static final String SVC_API_1 = "svc-api-1";
    public static final String SVC_API_2 = "svc-api-2";
    public static final String PROD_INFO_1 = "prod-info-1";
    public static final String PROD_INFO_2 = "prod-info-2";
    public static final String CONS_INFO_1 = "cons-info-1";
    public static final String CONS_INFO_2 = "cons-info-2";
    public static final String INFO_CONSUMER_1 = "info-consumer-1";
    public static final String INFO_CONSUMER_2 = "info-consumer-2";
    public static final String INFO_PRODUCER_1 = "info-producer-1";
    public static final String INFO_PRODUCER_2 = "info-producer-2";
    @Autowired
    RappInstanceValidationHandler rappValidationHandler;

    @Test
    void testrAppInstanceValidationSuccessAcmInstance() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance(K8S_INSTANCE);
        rappInstance.setAcm(rappACMInstance);
        assertDoesNotThrow(() -> rappValidationHandler.validateRappInstance(rapp, rappInstance));
    }

    @Test
    void testrAppInstanceValidationSuccessSmeInstance() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction(PROVIDER_1);
        rappSMEInstance.setServiceApis(SVC_API_1);
        rappSMEInstance.setInvokers(INV_1);
        rappInstance.setSme(rappSMEInstance);
        assertDoesNotThrow(() -> rappValidationHandler.validateRappInstance(rapp, rappInstance));
    }

    @Test
    void testrAppInstanceValidationSuccessDmeInstance() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of(PROD_INFO_1));
        rappDMEInstance.setInfoTypeConsumer(CONS_INFO_1);
        rappDMEInstance.setInfoConsumer(INFO_CONSUMER_1);
        rappDMEInstance.setInfoProducer(INFO_PRODUCER_1);
        rappInstance.setDme(rappDMEInstance);
        assertDoesNotThrow(() -> rappValidationHandler.validateRappInstance(rapp, rappInstance));
    }

    @Test
    void testAppInstanceValidationSuccessAllInstances() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();

        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance(K8S_INSTANCE);
        rappInstance.setAcm(rappACMInstance);

        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction(PROVIDER_1);
        rappSMEInstance.setServiceApis(SVC_API_1);
        rappSMEInstance.setInvokers(INV_1);
        rappInstance.setSme(rappSMEInstance);

        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of(PROD_INFO_1));
        rappDMEInstance.setInfoTypeConsumer(CONS_INFO_1);
        rappDMEInstance.setInfoConsumer(INFO_CONSUMER_1);
        rappDMEInstance.setInfoProducer(INFO_PRODUCER_1);
        rappInstance.setDme(rappDMEInstance);

        assertDoesNotThrow(() -> rappValidationHandler.validateRappInstance(rapp, rappInstance));
    }

    @Test
    void testrAppInstanceValidationFailureOnRappState() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance(K8S_INSTANCE);
        rappInstance.setAcm(rappACMInstance);
        rapp.setState(RappState.COMMISSIONED);

        RappValidationException rappValidationException = assertThrows(RappValidationException.class,
                () -> rappValidationHandler.validateRappInstance(rapp, rappInstance));
        assertEquals("Unable to create rApp instance as rApp is not in PRIMED state",
                rappValidationException.getMessage());
    }

    @Test
    void testrAppInstanceValidationFailureOnAcmPayload() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance("invalid-instance");
        rappInstance.setAcm(rappACMInstance);

        RappValidationException rappValidationException = assertThrows(RappValidationException.class,
                () -> rappValidationHandler.validateRappInstance(rapp, rappInstance));
        assertEquals("Invalid ACM instance in the rApp instance payload.", rappValidationException.getMessage());
    }

    @Test
    void testrAppInstanceValidationFailureOnSmePayload() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction("invalid-provider");
        rappSMEInstance.setServiceApis(SVC_API_1);
        rappSMEInstance.setInvokers(INV_1);
        rappInstance.setSme(rappSMEInstance);

        RappValidationException rappValidationException = assertThrows(RappValidationException.class,
                () -> rappValidationHandler.validateRappInstance(rapp, rappInstance));
        assertEquals("Invalid SME provider function in the rApp instance payload.",
                rappValidationException.getMessage());
    }

    @Test
    void testrAppInstanceValidationFailureOnDmePayload() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of("invalid-prod-info"));
        rappDMEInstance.setInfoTypeConsumer(CONS_INFO_1);
        rappDMEInstance.setInfoConsumer(INFO_CONSUMER_1);
        rappDMEInstance.setInfoProducer(INFO_PRODUCER_1);
        rappInstance.setDme(rappDMEInstance);

        RappValidationException rappValidationException = assertThrows(RappValidationException.class,
                () -> rappValidationHandler.validateRappInstance(rapp, rappInstance));
        assertEquals("Invalid DME info types producer in the rApp instance payload.",
                rappValidationException.getMessage());
    }

    Rapp getRapp() {
        RappResources rappResources = new RappResources();
        RappResources.ACMResources acmResources =
                RappResources.ACMResources.builder().compositionInstances(Set.of(K8S_INSTANCE, A1PMS_INSTANCE)).build();
        rappResources.setAcm(acmResources);
        RappResources.SMEResources smeResources =
                RappResources.SMEResources.builder().providerFunctions(Set.of(PROVIDER_1, PROVIDER_2))
                        .serviceApis(Set.of(SVC_API_1, SVC_API_2)).invokers(Set.of(INV_1, INV_2)).build();
        rappResources.setSme(smeResources);
        RappResources.DMEResources dmeResources =
                RappResources.DMEResources.builder().producerInfoTypes(Set.of(PROD_INFO_1, PROD_INFO_2))
                        .consumerInfoTypes(Set.of(CONS_INFO_1, CONS_INFO_2))
                        .infoConsumers(Set.of(INFO_CONSUMER_1, INFO_CONSUMER_2))
                        .infoProducers(Set.of(INFO_PRODUCER_1, INFO_PRODUCER_2)).build();
        rappResources.setDme(dmeResources);

        return Rapp.builder().name("").state(RappState.PRIMED).rappResources(rappResources).build();
    }
}
