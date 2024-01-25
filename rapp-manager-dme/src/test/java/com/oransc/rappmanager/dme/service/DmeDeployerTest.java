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

package com.oransc.rappmanager.dme.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.dme.configuration.DmeConfiguration;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappDmeResourceBuilder;
import com.oransc.rappmanager.models.rapp.RappResources;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachineConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(classes = {DmeConfiguration.class, DmeDeployer.class, BeanTestConfiguration.class,
        RappCsarConfigurationHandler.class, RappCacheService.class, RappInstanceStateMachineConfig.class,
        RappInstanceStateMachine.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
class DmeDeployerTest {

    MockRestServiceServer mockServer;
    @SpyBean
    DmeDeployer dmeDeployer;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    DmeConfiguration dmeConfiguration;

    RappDmeResourceBuilder rappDmeResourceBuilder = new RappDmeResourceBuilder();

    private static final String validRappFile = "valid-rapp-package.csar";
    private static final String validRappFileNewInfoType = "valid-rapp-package-new-info-type.csar";
    String validCsarFileLocation = "src/test/resources/";
    ObjectMapper objectMapper = new ObjectMapper();

    String URI_INFO_TYPES, URI_INFO_TYPE, URI_INFO_PRODUCER, URI_INFO_CONSUMER;

    @BeforeAll
    void initACMURI() {
        URI_INFO_TYPES = dmeConfiguration.getBaseUrl() + "/data-producer/v1/info-types";
        URI_INFO_TYPE = dmeConfiguration.getBaseUrl() + "/data-producer/v1/info-types/%s";
        URI_INFO_PRODUCER = dmeConfiguration.getBaseUrl() + "/data-producer/v1/info-producers/%s";
        URI_INFO_CONSUMER = dmeConfiguration.getBaseUrl() + "/data-consumer/v1/info-jobs/%s";
    }

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @ParameterizedTest
    @MethodSource("getSuccessParamsWithUnavailableInfoTypes")
    void testPrimeRappSuccessWithUnavailableInfoType(String rappFile, boolean isSuccess)
            throws JsonProcessingException {
        RappResources rappResources = rappDmeResourceBuilder.getResources();
        Rapp rapp = getRapp(Optional.empty());
        rapp.setPackageName(rappFile);
        rapp.setRappResources(rappResources);
        List<String> infoTypes = List.of();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_INFO_TYPES)).andExpect(method(HttpMethod.GET)).andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(infoTypes)));
        assertTrue(dmeDeployer.primeRapp(rapp));
        if (rappFile.equals(validRappFileNewInfoType)) {
            mockServer.verify();
        }
        if (isSuccess) {
            assertNull(rapp.getReason());
        } else {
            assertNotNull(rapp.getReason());
        }
    }

    private static Stream<Arguments> getSuccessParamsWithUnavailableInfoTypes() {
        return Stream.of(Arguments.of(validRappFile, true), Arguments.of(validRappFileNewInfoType, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {validRappFile, validRappFileNewInfoType})
    void testPrimeRappSuccessWithValidInfoType(String rappFile) throws JsonProcessingException {
        RappResources rappResources = rappDmeResourceBuilder.getResources();
        Rapp rapp = getRapp(Optional.empty());
        rapp.setPackageName(rappFile);
        rapp.setRappResources(rappResources);
        List<String> infoTypes = List.of("new-info-type-not-available");
        mockServer.expect(ExpectedCount.once(), requestTo(URI_INFO_TYPES)).andExpect(method(HttpMethod.GET)).andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(infoTypes)));
        assertTrue(dmeDeployer.primeRapp(rapp));
        if (rappFile.equals(validRappFileNewInfoType)) {
            mockServer.verify();
        }
        assertNull(rapp.getReason());
    }

    @Test
    void testPrimeRappWithoutDme() throws JsonProcessingException {
        RappResources rappResources = rappDmeResourceBuilder.getResources();
        rappResources.setDme(null);
        Rapp rapp = getRapp(Optional.empty());
        rapp.setPackageName(validRappFile);
        rapp.setRappResources(rappResources);
        assertTrue(dmeDeployer.primeRapp(rapp));
    }

    @Test
    void testPrimeRappFailure() {
        RappResources rappResources = rappDmeResourceBuilder.getResources();
        RappResources.DMEResources dme = rappResources.getDme();
        Set<String> infoProducers = new HashSet<>(rappResources.getDme().getInfoProducers());
        infoProducers.add("invalid-producer-not-available-in-rapp");
        dme.setInfoProducers(infoProducers);
        rappResources.setDme(dme);
        Rapp rapp = getRapp(Optional.empty());
        rapp.setRappResources(rappResources);
        assertFalse(dmeDeployer.primeRapp(rapp));
        assertFalse(rapp.getReason().isEmpty());
    }

    @Test
    void testDeprimeRapp() {
        Rapp rapp = getRapp(Optional.empty());
        assertTrue(dmeDeployer.deprimeRapp(rapp));
        assertNull(rapp.getReason());
    }

    @Test
    void testDeployrAppInstanceSuccess() {
        Rapp rapp = getRapp(Optional.empty());
        RappInstance rappInstance = rappDmeResourceBuilder.getRappInstance();
        assertTrue(dmeDeployer.deployRappInstance(rapp, rappInstance));
    }

    @Test
    void testUndeployrAppInstanceSuccess() {
        Rapp rapp = getRapp(Optional.empty());
        rapp.setState(RappState.PRIMED);
        RappInstance rappInstance = rappDmeResourceBuilder.getRappInstance();
        assertTrue(dmeDeployer.undeployRappInstance(rapp, rappInstance));
    }

    Rapp getRapp(Optional<UUID> rappOptional) {
        return Rapp.builder().rappId(rappOptional.orElse(UUID.randomUUID())).name("").packageName(validRappFile)
                       .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
    }
}
