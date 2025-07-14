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
import org.oransc.rappmanager.models.rappinstance.RappACMInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {AcmPayloadValidator.class})
class AcmPayloadValidatorTest {

    public static final String K8S_INSTANCE = "k8s-instance";
    @Autowired
    AcmPayloadValidator acmPayloadValidator;

    @Test
    void testAcmPayloadValidationSuccess() {
        RappResources rappResources = new RappResources();
        RappResources.ACMResources rappACMResources =
                RappResources.ACMResources.builder().compositionInstances(Set.of(K8S_INSTANCE)).build();
        rappResources.setAcm(rappACMResources);
        Rapp rapp = Rapp.builder().rappResources(rappResources).build();
        RappInstance rappInstance = new RappInstance();
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance(K8S_INSTANCE);
        rappInstance.setAcm(rappACMInstance);
        assertDoesNotThrow(() -> acmPayloadValidator.validate(rapp, rappInstance));
    }

    @Test
    void testAcmPayloadValidationWithNull() {
        RappResources rappResources = new RappResources();
        RappResources.ACMResources rappACMResources =
                RappResources.ACMResources.builder().compositionInstances(Set.of(K8S_INSTANCE)).build();
        rappResources.setAcm(rappACMResources);
        Rapp rapp = Rapp.builder().rappResources(rappResources).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setAcm(null);
        assertDoesNotThrow(() -> acmPayloadValidator.validate(rapp, rappInstance));
    }

    @Test
    void testAcmPayloadValidationSuccessNoAcm() {
        Rapp rapp = Rapp.builder().build();
        RappInstance rappInstance = new RappInstance();
        assertDoesNotThrow(() -> acmPayloadValidator.validate(rapp, rappInstance));
    }

    @Test
    void testAcmPayloadValidationFailure() {
        RappResources rappResources = new RappResources();
        RappResources.ACMResources rappACMResources =
                RappResources.ACMResources.builder().compositionInstances(Set.of(K8S_INSTANCE)).build();
        rappResources.setAcm(rappACMResources);
        Rapp rapp = Rapp.builder().rappResources(rappResources).build();
        RappInstance rappInstance = new RappInstance();
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance("invalid-instance");
        rappInstance.setAcm(rappACMInstance);
        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> acmPayloadValidator.validate(rapp, rappInstance));
        assertEquals("Invalid ACM instance in the rApp instance payload.", rappValidationException.getMessage());
    }
}
