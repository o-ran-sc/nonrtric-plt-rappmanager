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

package com.oransc.rappmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.acm.configuration.ACMConfiguration;
import com.oransc.rappmanager.acm.service.AcmDeployer;
import com.oransc.rappmanager.configuration.RappManagerConfiguration;
import com.oransc.rappmanager.models.Rapp;
import com.oransc.rappmanager.models.RappEvent;
import com.oransc.rappmanager.models.RappState;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.statemachine.RappStateMachine;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.http.entity.ContentType;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "rappmanager.csarlocation=src/test/resources")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
public class AcmDeployerTest {

    MockRestServiceServer mockServer;
    @SpyBean
    AcmDeployer acmDeployer;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    ACMConfiguration acmConfiguration;
    @Autowired
    RappManagerConfiguration rappManagerConfiguration;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    RappCacheService rappCacheService;
    @SpyBean
    RappStateMachine rappStateMachine;
    private final String validRappFile = "valid-rapp-package.csar";
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
        String compositionPayload = Files.readString(Path.of(acmConfiguration.getCompositionDefinitionLocation()));
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
    void testCreateCompositionFailure() throws IOException {
        String compositionPayload = Files.readString(Path.of(acmConfiguration.getCompositionDefinitionLocation()));
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
    void testDeployRapp() throws Exception {

        UUID compositionId = UUID.randomUUID();
        when(acmDeployer.getCompositionId()).thenReturn(compositionId);
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name(rappId.toString()).packageName(validRappFile).compositionId(compositionId)
                            .packageLocation(rappManagerConfiguration.getCsarLocation()).state(RappState.ONBOARDED)
                            .build();
        onBoardRappCsar(rappId);
        InstantiationResponse instantiationResponse = new InstantiationResponse();
        instantiationResponse.setInstanceId(instanceId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCES, compositionId)))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(instantiationResponse)));

        mockServer.expect(ExpectedCount.once(),
                        requestTo(acmConfiguration.getBaseUrl() + "compositions/" + compositionId + "/instances/" + instanceId))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));


        boolean rappDeployStateActual = acmDeployer.deployRapp(rapp);
        assertTrue(rappDeployStateActual);
        mockServer.verify();
    }

    @Test
    void testDeployRappFailure() throws Exception {
        UUID compositionId = UUID.randomUUID();
        when(acmDeployer.getCompositionId()).thenReturn(compositionId);
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name(rappId.toString()).packageName(validRappFile).compositionId(compositionId)
                            .packageLocation(rappManagerConfiguration.getCsarLocation()).state(RappState.ONBOARDED)
                            .build();
        onBoardRappCsar(rappId);
        InstantiationResponse instantiationResponse = new InstantiationResponse();
        instantiationResponse.setInstanceId(instanceId);
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCES, compositionId)))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        boolean rappDeployStateActual = acmDeployer.deployRapp(rapp);
        mockServer.verify();
        assertFalse(rappDeployStateActual);
    }

    @Test
    void testUndeployRapp() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        when(acmDeployer.getCompositionId()).thenReturn(compositionId);
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(rappId.toString()).packageName(validRappFile)
                            .compositionId(compositionId).compositionInstanceId(instanceId).state(RappState.DEPLOYED)
                            .build();
        rappCacheService.putRapp(rapp);
        rappStateMachine.onboardRapp(rappId);

        expectAcmGetInstanceToReturnDeployedState(compositionId, instanceId, DeployState.DEPLOYED, LockState.LOCKED,
                ExpectedCount.once());

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));

        expectAcmGetInstanceToReturnDeployedState(compositionId, instanceId, DeployState.UNDEPLOYED, LockState.UNLOCKED,
                ExpectedCount.once());

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));


        boolean rappUndeployStateActual = acmDeployer.undeployRapp(rapp);
        mockServer.verify();
        assertTrue(rappUndeployStateActual);
    }

    @Test
    void testUndeployRappFailure() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        when(acmDeployer.getCompositionId()).thenReturn(compositionId);
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name(rappId.toString()).packageName(validRappFile).compositionId(compositionId)
                            .compositionInstanceId(instanceId).state(RappState.DEPLOYED).build();
        rappCacheService.putRapp(rapp);

        expectAcmGetInstanceToReturnDeployedState(compositionId, instanceId, DeployState.DEPLOYED, LockState.LOCKED,
                ExpectedCount.once());

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.ACCEPTED));

        expectAcmGetInstanceToReturnDeployedState(compositionId, instanceId, DeployState.UNDEPLOYING,
                LockState.UNLOCKING, ExpectedCount.manyTimes());

        boolean rappUndeployStateActual = acmDeployer.undeployRapp(rapp);
        mockServer.verify();
        assertFalse(rappUndeployStateActual);
    }

    @Test
    void testSyncRappStatus() throws JsonProcessingException {
        UUID compositionId = UUID.randomUUID();
        when(acmDeployer.getCompositionId()).thenReturn(compositionId);
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(rappId.toString()).packageName(validRappFile)
                            .compositionId(compositionId).compositionInstanceId(instanceId).state(RappState.DEPLOYED)
                            .build();
        rappCacheService.putRapp(rapp);
        rappStateMachine.onboardRapp(rappId);

        expectAcmGetInstanceToReturnDeployedState(compositionId, instanceId, DeployState.UNDEPLOYING,
                LockState.UNLOCKING, ExpectedCount.once());

        acmDeployer.syncRappStatus(rapp);
        mockServer.verify();
        verify(rappStateMachine, times(1)).sendRappEvent(rapp, RappEvent.UNDEPLOYING);
    }

    @Test
    void testSyncRappStatusFailure() {
        UUID compositionId = UUID.randomUUID();
        when(acmDeployer.getCompositionId()).thenReturn(compositionId);
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().name(rappId.toString()).packageName(validRappFile).compositionId(compositionId)
                            .compositionInstanceId(instanceId).state(RappState.DEPLOYED).build();
        rappCacheService.putRapp(rapp);

        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_ACM_INSTANCE, compositionId, instanceId)))
                .andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        acmDeployer.syncRappStatus(rapp);
        mockServer.verify();
        verify(rappStateMachine, never()).sendRappEvent(any(), any());
    }

    void expectAcmGetInstanceToReturnDeployedState(UUID compositionId, UUID instanceId, DeployState deployState,
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

    void onBoardRappCsar(UUID rappId) throws Exception {
        String rappCsarPath = rappManagerConfiguration.getCsarLocation() + File.separator + validRappFile;
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", validRappFile, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}/onboard", rappId).file(multipartFile))
                .andExpect(status().isAccepted());
    }


}
