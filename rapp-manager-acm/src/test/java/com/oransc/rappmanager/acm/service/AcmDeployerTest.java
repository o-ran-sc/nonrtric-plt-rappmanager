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

package com.oransc.rappmanager.acm.service;

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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.acm.configuration.ACMConfiguration;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.rapp.RappEvent;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rapp.RappResourceBuilder;
import com.oransc.rappmanager.models.rapp.RappResources;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachineConfig;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
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

@SpringBootTest(classes = {BeanTestConfiguration.class, ACMConfiguration.class, AcmDeployer.class,
        RappCsarConfigurationHandler.class, RappCacheService.class, RappInstanceStateMachineConfig.class,
        RappInstanceStateMachine.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
class AcmDeployerTest {

    MockRestServiceServer mockServer;
    @SpyBean
    AcmDeployer acmDeployer;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    ACMConfiguration acmConfiguration;
    @SpyBean
    RappInstanceStateMachine rappInstanceStateMachine;
    @Autowired
    RappCsarConfigurationHandler rappCsarConfigurationHandler;

    RappResourceBuilder rappResourceBuilder = new RappResourceBuilder();
    private final String validRappFile = "valid-rapp-package.csar";
    String validCsarFileLocation = "src/test/resources/";
    ObjectMapper objectMapper = new ObjectMapper();
    String URI_ACM_COMPOSITIONS, URI_ACM_COMPOSITION, URI_ACM_INSTANCES, URI_ACM_INSTANCE;

    @BeforeAll
    void initACMURI() {
        URI_ACM_COMPOSITIONS = acmConfiguration.getBaseUrl() + "compositions";
        URI_ACM_COMPOSITION = acmConfiguration.getBaseUrl() + "compositions/%s";
        URI_ACM_INSTANCES = acmConfiguration.getBaseUrl() + "compositions/%s/instances";
        URI_ACM_INSTANCE = acmConfiguration.getBaseUrl() + "compositions/%s/instances/%s";
    }

    @BeforeEach
    public void init() {
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
        mockServer.expect(ExpectedCount.once(), requestTo(URI_ACM_COMPOSITIONS)).andExpect(method(HttpMethod.POST))
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
        mockServer.expect(ExpectedCount.once(), requestTo(URI_ACM_COMPOSITIONS)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        CommissioningResponse commissioningResponseActual = acmDeployer.createComposition(compositionPayload);
        mockServer.verify();
        assertNull(commissioningResponseActual);
    }

    @Test
    void testCompositionPriming() {
        UUID compositionId = UUID.randomUUID();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));

        acmDeployer.primeACMComposition(compositionId, PrimeOrder.PRIME);
        mockServer.verify();
    }

    @Test
    void testDeployRappInstance() throws Exception {

        UUID compositionId = UUID.randomUUID();
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name(rappId.toString()).packageName(validRappFile).compositionId(compositionId)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        InstantiationResponse instantiationResponse = new InstantiationResponse();
        instantiationResponse.setInstanceId(instanceId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCES, compositionId)))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(instantiationResponse)));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(acmConfiguration.getBaseUrl() + "compositions/" + compositionId + "/instances/" + instanceId))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));
        boolean rappDeployStateActual = acmDeployer.deployRappInstance(rapp, rappResourceBuilder.getRappInstance());
        assertTrue(rappDeployStateActual);
        mockServer.verify();
    }

    @Test
    void testDeployRappInstanceFailure() throws Exception {
        UUID compositionId = UUID.randomUUID();
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name(rappId.toString()).packageName(validRappFile).compositionId(compositionId)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        InstantiationResponse instantiationResponse = new InstantiationResponse();
        instantiationResponse.setInstanceId(instanceId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCES, compositionId)))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        boolean rappDeployStateActual = acmDeployer.deployRappInstance(rapp, rappResourceBuilder.getRappInstance());
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

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));

        expectAcmGetInstanceToReturnState(compositionId, instanceId, DeployState.UNDEPLOYED, LockState.UNLOCKED,
                ExpectedCount.once());

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));

        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(instanceId);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean rappUndeployStateActual = acmDeployer.undeployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertTrue(rappUndeployStateActual);
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

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
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
    void testSyncRappInstanceStatus() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        expectAcmGetInstanceToReturnState(compositionId, instanceId, DeployState.UNDEPLOYING, LockState.UNLOCKING,
                ExpectedCount.once());
        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(instanceId);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        acmDeployer.syncRappInstanceStatus(compositionId, rappInstance);
        mockServer.verify();
        verify(rappInstanceStateMachine, times(1)).sendRappInstanceEvent(rappInstance, RappEvent.UNDEPLOYING);
    }

    @Test
    void testSyncRappStatusFailure() {
        UUID compositionId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
                .andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        RappInstance rappInstance = rappResourceBuilder.getRappInstance();
        rappInstance.getAcm().setAcmInstanceId(instanceId);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        acmDeployer.syncRappInstanceStatus(compositionId, rappInstance);
        mockServer.verify();
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
        mockServer.expect(ExpectedCount.once(), requestTo(URI_ACM_COMPOSITIONS)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                    .body(objectMapper.writeValueAsString(commissioningResponseExpected)));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
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
        mockServer.expect(ExpectedCount.once(), requestTo(URI_ACM_COMPOSITIONS)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                    .body(objectMapper.writeValueAsString(commissioningResponseExpected)));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        boolean primeRapp = acmDeployer.primeRapp(rapp);
        mockServer.verify();
        assertFalse(primeRapp);
        assertEquals(RappState.COMMISSIONED, rapp.getState());
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
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.OK));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(commissioningResponseExpected)));

        boolean deprimeRapp = acmDeployer.deprimeRapp(rapp);
        mockServer.verify();
        assertTrue(deprimeRapp);
    }

    @Test
    void testDeprimeFailureRapp() {
        UUID compositionId = UUID.randomUUID();
        RappResources rappResources = rappResourceBuilder.getResources();
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .compositionId(compositionId).rappResources(rappResources).build();

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.OK));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        boolean deprimeRapp = acmDeployer.deprimeRapp(rapp);
        mockServer.verify();
        assertFalse(deprimeRapp);
    }

    @Test
    void testDeleteComposition() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        CommissioningResponse commissioningResponseExpected = new CommissioningResponse();
        commissioningResponseExpected.setCompositionId(compositionId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
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
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_COMPOSITION, compositionId)))
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

        mockServer.expect(expectedCount, requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
                .andExpect(method(HttpMethod.GET)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(automationCompositionDeployed)));
    }
}
