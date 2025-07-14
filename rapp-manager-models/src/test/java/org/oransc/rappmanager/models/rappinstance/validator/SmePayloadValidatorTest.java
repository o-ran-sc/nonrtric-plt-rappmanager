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
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {SmePayloadValidator.class})
class SmePayloadValidatorTest {

    public static final String PROVIDER_FUNCTION_1 = "provider-function-1";
    public static final String SERVICE_API_1 = "service-api-1";
    public static final String INVOKER_1 = "invoker-1";
    @Autowired
    private SmePayloadValidator smePayloadValidator;

    @Test
    void testSmePayloadValidatorSuccess() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction(PROVIDER_FUNCTION_1);
        rappSMEInstance.setServiceApis(SERVICE_API_1);
        rappSMEInstance.setInvokers(INVOKER_1);
        rappInstance.setSme(rappSMEInstance);
        assertDoesNotThrow(() -> smePayloadValidator.validate(rapp, rappInstance));
    }

    @Test
    void testSmePayloadValidatorSuccessWithNull() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction(null);
        rappSMEInstance.setServiceApis(null);
        rappSMEInstance.setInvokers(INVOKER_1);
        rappInstance.setSme(rappSMEInstance);
        assertDoesNotThrow(() -> smePayloadValidator.validate(rapp, rappInstance));
    }

    @Test
    void testSmePayloadValidatorFailureInvalidProviderFunction() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction("invalid-provider-function");
        rappSMEInstance.setServiceApis(SERVICE_API_1);
        rappSMEInstance.setInvokers(INVOKER_1);
        rappInstance.setSme(rappSMEInstance);

        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> smePayloadValidator.validate(rapp, rappInstance));
        assertEquals("Invalid SME provider function in the rApp instance payload.",
                rappValidationException.getMessage());
    }

    @Test
    void testSmePayloadValidatorFailureInvalidServiceApis() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction(PROVIDER_FUNCTION_1);
        rappSMEInstance.setServiceApis("invalid-service-api");
        rappSMEInstance.setInvokers(INVOKER_1);
        rappInstance.setSme(rappSMEInstance);

        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> smePayloadValidator.validate(rapp, rappInstance));
        assertEquals("Invalid SME service APIs in the rApp instance payload.", rappValidationException.getMessage());
    }

    @Test
    void testSmePayloadValidatorFailureInvalidInvokers() {
        Rapp rapp = getRapp();
        RappInstance rappInstance = new RappInstance();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction(PROVIDER_FUNCTION_1);
        rappSMEInstance.setServiceApis(SERVICE_API_1);
        rappSMEInstance.setInvokers("invalid-invoker");
        rappInstance.setSme(rappSMEInstance);

        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> smePayloadValidator.validate(rapp, rappInstance));
        assertEquals("Invalid SME invokers in the rApp instance payload.", rappValidationException.getMessage());
    }


    private Rapp getRapp() {
        RappResources rappResources = new RappResources();
        RappResources.SMEResources rappSMEResources = RappResources.SMEResources.builder().providerFunctions(
                        Set.of(PROVIDER_FUNCTION_1, "provider-function-2")).serviceApis(Set.of(SERVICE_API_1, "service-api-2"))
                                                              .invokers(Set.of(INVOKER_1, "invoker-2")).build();

        rappResources.setSme(rappSMEResources);
        return Rapp.builder().rappResources(rappResources).build();
    }
}
