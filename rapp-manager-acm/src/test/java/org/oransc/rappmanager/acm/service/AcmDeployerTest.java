/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.acm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.oransc.rappmanager.acm.configuration.ACMConfiguration;
import org.oransc.rappmanager.dme.service.DmeAcmInterceptor;
import org.oransc.rappmanager.models.cache.RappCacheService;
import org.oransc.rappmanager.models.configuration.RappsEnvironmentConfiguration;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappEvent;
import org.oransc.rappmanager.models.rapp.RappResourceBuilder;
import org.oransc.rappmanager.models.rapp.RappResources;
import org.oransc.rappmanager.models.rapp.RappState;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import org.oransc.rappmanager.models.statemachine.RappInstanceStateMachineConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
        classes = {BeanTestConfiguration.class, ACMConfiguration.class, AcmDeployer.class, DmeAcmInterceptor.class,
                RappsEnvironmentConfiguration.class, RappCsarConfigurationHandler.class, RappCacheService.class,
                RappInstanceStateMachineConfig.class, RappInstanceStateMachine.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
class AcmDeployerTest {

    MockRestServiceServer mockServer;
    @MockitoSpyBean
    AcmDeployer acmDeployer;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    ACMConfiguration acmConfiguration;
    @MockitoSpyBean
    RappInstanceStateMachine rappInstanceStateMachine;
    @Autowired
    RappCsarConfigurationHandler rappCsarConfigurationHandler;
    @Autowired
    ObjectMapper objectMapper;

    RappResourceBuilder rappResourceBuilder = new RappResourceBuilder();
    private final String validRappFile = "valid-rapp-package.csar";
    String validCsarFileLocation = "src/test/resources/";
    String uriAcmCompositions, uriAcmComposition, uriAcmInstances, uriAcmInstance;

    @BeforeAll
    void initACMURI() {
        uriAcmCompositions = acmConfiguration.getBaseUrl() + "compositions";
        uriAcmComposition = acmConfiguration.getBaseUrl() + "compositions/%s";
        uriAcmInstances = acmConfiguration.getBaseUrl() + "compositions/%s/instances";
        uriAcmInstance = acmConfiguration.getBaseUrl() + "compositions/%s/instances/%s";
    }

    @BeforeEach
    void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testCreateComposition() throws IOException {
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .rappResources(rappResources).build();

        String compositionPayload = rappCsarConfigurationHandler.getAcmCompositionPayload(rapp);

        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        commissioningResponseExpected.setCompositionId(UUID.randomUUID());
        mockServer.expect(ExpectedCount.once(), requestTo(uriAcmCompositions)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                    .body(objectMapper.writeValueAsString(commissioningResponseExpected)));
        CommissioningResponse commissioningResponseActual = acmDeployer.createComposition(compositionPayload);
        mockServer.verify();
        assertEquals(commissioningResponseActual.getCompositionId(), commissioningResponseExpected.getCompositionId());
    }

    @Test
    void testCreateCompositionFailure() {
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .rappResources(rappResources).build();
        String compositionPayload = rappCsarConfigurationHandler.getAcmCompositionPayload(rapp);
        mockServer.expect(ExpectedCount.once(), requestTo(uriAcmCompositions)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        CommissioningResponse commissioningResponseActual = acmDeployer.createComposition(compositionPayload);
        mockServer.verify();
        assertNull(commissioningResponseActual);
    }

    @Test
    void testCompositionPriming() {
        UUID compositionId = UUID.randomUUID();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));

        acmDeployer.primeACMComposition(compositionId, PrimeOrder.PRIME);
        mockServer.verify();
    }

    @Test
    void testDeployRappInstance() throws Exception {

        UUID compositionId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).compositionId(compositionId)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        InstantiationResponse instantiationResponse = new InstantiationResponse();
        instantiationResponse.setInstanceId(instanceId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstances, compositionId)))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(instantiationResponse)));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(acmConfiguration.getBaseUrl() + "compositions/" + compositionId + "/instances/" + instanceId))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));
        boolean rappDeployStateActual = acmDeployer.deployRappInstance(rapp, rappInstance);
        assertTrue(rappDeployStateActual);
        mockServer.verify();
    }

    @Test
    void testDeployRappInstanceWithoutDmeInjection() throws Exception {

        UUID compositionId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).compositionId(compositionId)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.setDme(null);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        InstantiationResponse instantiationResponse = new InstantiationResponse();
        instantiationResponse.setInstanceId(instanceId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstances, compositionId)))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(instantiationResponse)));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(acmConfiguration.getBaseUrl() + "compositions/" + compositionId + "/instances/" + instanceId))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));
        boolean rappDeployStateActual = acmDeployer.deployRappInstance(rapp, rappInstance);
        assertTrue(rappDeployStateActual);
        mockServer.verify();
    }

    @Test
    void testDeployRappInstanceFailureWithNoInstanceId() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).compositionId(compositionId)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        InstantiationResponse instantiationResponse = new InstantiationResponse();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstances, compositionId)))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(instantiationResponse)));

        boolean rappDeployStateActual = acmDeployer.deployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertFalse(rappDeployStateActual);
    }

    @Test
    void testDeployRappInstanceFailure() {
        UUID compositionId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).compositionId(compositionId)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstances, compositionId)))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        boolean rappDeployStateActual = acmDeployer.deployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertFalse(rappDeployStateActual);
    }

    @Test
    void testUndeployRappInstance() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name("").packageName(validRappFile).compositionId(compositionId)
                            .state(RappState.PRIMED).build();

        expectAcmGetInstanceToReturnState(compositionId, instanceId, DeployState.DEPLOYED, LockState.LOCKED,
                ExpectedCount.once());

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstance, compositionId, instanceId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));

        expectAcmGetInstanceToReturnState(compositionId, instanceId, DeployState.UNDEPLOYED, LockState.UNLOCKED,
                ExpectedCount.once());

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstance, compositionId, instanceId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));

        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(instanceId);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean rappUndeployStateActual = acmDeployer.undeployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertTrue(rappUndeployStateActual);
        assertNull(rappInstance.getAcm().getAcmInstanceId());
    }

    @Test
    void testUndeployRappInstanceFailure() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name(rappId.toString()).packageName(validRappFile).compositionId(compositionId)
                            .state(RappState.PRIMED).build();

        expectAcmGetInstanceToReturnState(compositionId, instanceId, DeployState.DEPLOYED, LockState.LOCKED,
                ExpectedCount.once());

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstance, compositionId, instanceId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));

        expectAcmGetInstanceToReturnState(compositionId, instanceId, DeployState.UNDEPLOYING, LockState.UNLOCKING,
                ExpectedCount.manyTimes());

        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(instanceId);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean rappUndeployStateActual = acmDeployer.undeployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertFalse(rappUndeployStateActual);
    }

    @Test
    void testUndeployRappInstanceACMErrorFailure() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name(rappId.toString()).packageName(validRappFile).compositionId(compositionId)
                            .state(RappState.PRIMED).build();
        expectAcmGetInstanceToReturnState(compositionId, instanceId, DeployState.DEPLOYED, LockState.LOCKED,
                ExpectedCount.once());
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstance, compositionId, instanceId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));
        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format(uriAcmInstance, compositionId, instanceId))).andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(instanceId);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean rappUndeployStateActual = acmDeployer.undeployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertFalse(rappUndeployStateActual);
    }

    @ParameterizedTest
    @MethodSource("getAcmStatusEventMap")
    void testSyncRappInstanceStatus(DeployState deployState, LockState lockState, RappEvent rappEvent)
            throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        expectAcmGetInstanceToReturnState(compositionId, instanceId, deployState, lockState, ExpectedCount.once());
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(instanceId);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        acmDeployer.syncRappInstanceStatus(compositionId, rappInstance);
        mockServer.verify();
        verify(rappInstanceStateMachine, times(1)).sendRappInstanceEvent(rappInstance, rappEvent);
    }

    private static Stream<Arguments> getAcmStatusEventMap() {
        return Stream.of(Arguments.of(DeployState.UNDEPLOYING, LockState.UNLOCKING, RappEvent.UNDEPLOYING),
                Arguments.of(DeployState.DEPLOYED, LockState.LOCKED, RappEvent.ACMDEPLOYED),
                Arguments.of(DeployState.DEPLOYING, LockState.LOCKING, RappEvent.DEPLOYING),
                Arguments.of(DeployState.UNDEPLOYED, LockState.UNLOCKED, RappEvent.ACMUNDEPLOYED));
    }

    @Test
    void testSyncRappStatusFailure() {
        UUID compositionId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmInstance, compositionId, instanceId)))
                .andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(instanceId);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        acmDeployer.syncRappInstanceStatus(compositionId, rappInstance);
        mockServer.verify();
        verify(rappInstanceStateMachine, never()).sendRappInstanceEvent(any(), any());
    }

    @Test
    void testSyncRappStatusFailureAcmNull() {
        UUID compositionId = UUID.randomUUID();
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.setAcm(null);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        acmDeployer.syncRappInstanceStatus(compositionId, rappInstance);
        verify(rappInstanceStateMachine, never()).sendRappInstanceEvent(any(), any());
    }

    @Test
    void testSyncRappStatusFailureAcmInstanceIdNull() {
        UUID compositionId = UUID.randomUUID();
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(null);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        acmDeployer.syncRappInstanceStatus(compositionId, rappInstance);
        verify(rappInstanceStateMachine, never()).sendRappInstanceEvent(any(), any());
    }

    @Test
    void testPrimeRapp() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        commissioningResponseExpected.setCompositionId(compositionId);
        mockServer.expect(ExpectedCount.once(), requestTo(uriAcmCompositions)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                    .body(objectMapper.writeValueAsString(commissioningResponseExpected)));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));
        boolean primeRapp = acmDeployer.primeRapp(rapp);
        mockServer.verify();
        assertTrue(primeRapp);
    }

    @Test
    void testPrimeRappFailure() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        commissioningResponseExpected.setCompositionId(compositionId);
        mockServer.expect(ExpectedCount.once(), requestTo(uriAcmCompositions)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                    .body(objectMapper.writeValueAsString(commissioningResponseExpected)));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        boolean primeRapp = acmDeployer.primeRapp(rapp);
        mockServer.verify();
        assertFalse(primeRapp);
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testPrimeRappFailureWithoutCompositionId() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        mockServer.expect(ExpectedCount.once(), requestTo(uriAcmCompositions)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                    .body(objectMapper.writeValueAsString(commissioningResponseExpected)));
        boolean primeRapp = acmDeployer.primeRapp(rapp);
        mockServer.verify();
        assertFalse(primeRapp);
    }

    @Test
    void testDeprimeRapp() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        commissioningResponseExpected.setCompositionId(compositionId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.OK));
        AutomationCompositionDefinition automationCompositionDefinition =
                getAutomationCompositionDefinition(compositionId, AcTypeState.COMMISSIONED);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.GET)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(automationCompositionDefinition)));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(commissioningResponseExpected)));

        boolean deprimeRapp = acmDeployer.deprimeRapp(rapp);
        mockServer.verify();
        assertTrue(deprimeRapp);
    }

    @Test
    void testDeprimeRappClientRetry() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        commissioningResponseExpected.setCompositionId(compositionId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.OK));
        AutomationCompositionDefinition automationCompositionDefinition =
                getAutomationCompositionDefinition(compositionId, AcTypeState.DEPRIMING);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.GET)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(automationCompositionDefinition)));
        automationCompositionDefinition = getAutomationCompositionDefinition(compositionId, AcTypeState.COMMISSIONED);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.GET)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(automationCompositionDefinition)));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(commissioningResponseExpected)));

        boolean deprimeRapp = acmDeployer.deprimeRapp(rapp);
        mockServer.verify();
        assertTrue(deprimeRapp);
    }

    @Test
    void testDeprimeFailureRapp() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.OK));
        AutomationCompositionDefinition automationCompositionDefinition =
                getAutomationCompositionDefinition(compositionId, AcTypeState.COMMISSIONED);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.GET)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(automationCompositionDefinition)));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        boolean deprimeRapp = acmDeployer.deprimeRapp(rapp);
        mockServer.verify();
        assertFalse(deprimeRapp);
    }

    @Test
    void testDeprimeACMStatusFailureRapp() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.OK));
        AutomationCompositionDefinition automationCompositionDefinition =
                getAutomationCompositionDefinition(compositionId, AcTypeState.DEPRIMING);
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.GET)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(automationCompositionDefinition)));

        boolean deprimeRapp = acmDeployer.deprimeRapp(rapp);
        mockServer.verify();
        assertFalse(deprimeRapp);
    }

    @Test
    void testDeprimeACMStatusErrorRapp() {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.OK));
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.GET)).andRespond(withServerError());
        boolean deprimeRapp = acmDeployer.deprimeRapp(rapp);
        mockServer.verify();
        assertFalse(deprimeRapp);
    }

    @Test
    void testDeprimeExceptionRapp() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        boolean deprimeRapp = acmDeployer.deprimeRapp(rapp);
        assertFalse(deprimeRapp);
    }

    @Test
    void testDeleteComposition() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        commissioningResponseExpected.setCompositionId(compositionId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(commissioningResponseExpected)));
        CommissioningResponse commissioningResponse = acmDeployer.deleteComposition(compositionId);
        mockServer.verify();
        assertEquals(commissioningResponse.getCompositionId(), compositionId);
    }

    @Test
    void testDeleteCompositionFailure() {
        UUID compositionId = UUID.randomUUID();
        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        commissioningResponseExpected.setCompositionId(compositionId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(uriAcmComposition, compositionId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        CommissioningResponse commissioningResponse = acmDeployer.deleteComposition(compositionId);
        mockServer.verify();
        assertNull(commissioningResponse);
    }

    void expectAcmGetInstanceToReturnState(UUID compositionId, UUID instanceId, DeployState deployState,
            LockState lockState, ExpectedCount expectedCount) throws JsonProcessingException {
        AutomationComposition automationCompositionDeployed = new AutomationComposition();
        automationCompositionDeployed.setCompositionId(compositionId);
        automationCompositionDeployed.setInstanceId(instanceId);
        automationCompositionDeployed.setDeployState(deployState);
        automationCompositionDeployed.setLockState(lockState);

        mockServer.expect(expectedCount, requestTo(String.format(uriAcmInstance, compositionId, instanceId)))
                .andExpect(method(HttpMethod.GET)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(automationCompositionDeployed)));
    }

    AutomationCompositionDefinition getAutomationCompositionDefinition(UUID compositionId, AcTypeState acTypeState) {
        AutomationCompositionDefinition automationCompositionDefinition = new AutomationCompositionDefinition();
        automationCompositionDefinition.setCompositionId(compositionId);
        automationCompositionDefinition.setState(acTypeState);
        automationCompositionDefinition.setServiceTemplate(new ToscaServiceTemplate());
        automationCompositionDefinition.setLastMsg(TimestampHelper.now());
        return automationCompositionDefinition;
    }
}
