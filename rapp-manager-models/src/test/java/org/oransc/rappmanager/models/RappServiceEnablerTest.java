/*-
 * ============LICENSE_START======================================================================
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

package org.oransc.rappmanager.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.models.configuration.RappsEnvironmentConfiguration;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappResources;
import org.oransc.rappmanager.models.rappinstance.RappACMInstance;
import org.oransc.rappmanager.models.rappinstance.RappDMEInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(
        classes = {ObjectMapper.class, RappsEnvironmentConfiguration.class, RappCsarConfigurationHandler.class})
class RappServiceEnablerTest {

    @Autowired
    RappCsarConfigurationHandler rappCsarConfigurationHandler;
    String validCsarFileLocation = "src/test/resources/";
    private final String validRappFile = "valid-rapp-package.csar";

    @Test
    void testRappIsDmeAndSmeEnabled() {
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(
                Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build());
        Rapp rapp = Rapp.builder().name("").rappResources(rappResources).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).build();
        assertTrue(rapp.isDMEEnabled());
        assertTrue(rapp.isSMEEnabled());
    }

    @Test
    void testRappIsNotDmeEnabled() {
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(
                Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build());
        Rapp rapp = Rapp.builder().name("").rappResources(rappResources).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).build();
        rapp.getRappResources().setDme(null);
        assertFalse(rapp.isDMEEnabled());
    }

    @Test
    void testRappIsNotSmeEnabled() {
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(
                Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build());
        Rapp rapp = Rapp.builder().name("").rappResources(rappResources).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).build();
        rapp.getRappResources().setSme(null);
        assertFalse(rapp.isSMEEnabled());
    }

    @Test
    void testRappIsNotDmeEnabledWithFolder() {
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(
                Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build());
        Rapp rapp = Rapp.builder().name("").rappResources(rappResources).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).build();
        rapp.getRappResources().getDme().setConsumerInfoTypes(Set.of());
        rapp.getRappResources().getDme().setProducerInfoTypes(Set.of());
        rapp.getRappResources().getDme().setInfoConsumers(Set.of());
        rapp.getRappResources().getDme().setInfoProducers(Set.of());
        assertFalse(rapp.isDMEEnabled());
    }

    @Test
    void testRappIsNotSmeEnabledWithFolder() {
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(
                Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build());
        Rapp rapp = Rapp.builder().name("").rappResources(rappResources).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).build();
        rapp.getRappResources().getSme().setProviderFunctions(Set.of());
        rapp.getRappResources().getSme().setServiceApis(Set.of());
        rapp.getRappResources().getSme().setInvokers(Set.of());
        assertFalse(rapp.isSMEEnabled());
    }

    @Test
    void testRappInstanceIsDmeAndSmeEnabled() {
        RappInstance rappInstance = new RappInstance();
        rappInstance.setAcm(new RappACMInstance());
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of("prod1", "prod2"));
        rappDMEInstance.setInfoTypeConsumer("cons");
        rappInstance.setDme(rappDMEInstance);
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction("func1");
        rappInstance.setSme(rappSMEInstance);
        assertTrue(rappInstance.isDMEEnabled());
        assertTrue(rappInstance.isSMEEnabled());
    }

    @Test
    void testRappInstanceIsNotDmeEnabled() {
        RappInstance rappInstance = new RappInstance();
        rappInstance.setAcm(new RappACMInstance());
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction("func1");
        rappInstance.setSme(rappSMEInstance);
        assertFalse(rappInstance.isDMEEnabled());
    }

    @Test
    void testRappInstanceIsNotSmeEnabled() {
        RappInstance rappInstance = new RappInstance();
        rappInstance.setAcm(new RappACMInstance());
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of("prod1", "prod2"));
        rappDMEInstance.setInfoTypeConsumer("cons");
        rappInstance.setDme(rappDMEInstance);
        assertFalse(rappInstance.isSMEEnabled());
    }

    @Test
    void testRappInstanceIsNotDmeEnabledWithoutContent() {
        RappInstance rappInstance = new RappInstance();
        rappInstance.setAcm(new RappACMInstance());
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction("func1");
        rappInstance.setSme(rappSMEInstance);
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappInstance.setDme(rappDMEInstance);
        assertFalse(rappInstance.isDMEEnabled());
    }

    @Test
    void testRappInstanceIsNotSmeEnabledWithoutContent() {
        RappInstance rappInstance = new RappInstance();
        rappInstance.setAcm(new RappACMInstance());
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of("prod1", "prod2"));
        rappDMEInstance.setInfoTypeConsumer("cons");
        rappInstance.setDme(rappDMEInstance);
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappInstance.setSme(rappSMEInstance);
        assertFalse(rappInstance.isSMEEnabled());
    }
}
