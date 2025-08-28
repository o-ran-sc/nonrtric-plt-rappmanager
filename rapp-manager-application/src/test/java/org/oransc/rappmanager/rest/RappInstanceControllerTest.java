/*
 * ============LICENSE_START======================================================================
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

package org.oransc.rappmanager.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.oransc.rappmanager.acm.service.AcmDeployer;
import org.oransc.rappmanager.dme.service.DmeDeployer;
import org.oransc.rappmanager.models.cache.RappCacheService;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappResources;
import org.oransc.rappmanager.models.rapp.RappState;
import org.oransc.rappmanager.models.rappinstance.DeployOrder;
import org.oransc.rappmanager.models.rappinstance.RappACMInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstanceDeployOrder;
import org.oransc.rappmanager.models.rappinstance.RappInstanceState;
import org.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import org.oransc.rappmanager.sme.service.SmeDeployer;
import org.oransc.rappmanager.sme.service.SmeLifecycleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RappInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RappCacheService rappCacheService;

    @Autowired
    private RappInstanceStateMachine rappInstanceStateMachine;

    @MockitoBean
    AcmDeployer acmDeployer;

    @MockitoBean
    SmeDeployer smeDeployer;

    @MockitoBean
    DmeDeployer dmeDeployer;

    @MockitoBean
    SmeLifecycleManager smeLifecycleManager;

    private final String validRappFile = "valid-rapp-package.csar";

    private final String validCsarFileLocation = "src/test/resources/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetAllRappInstances() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}/instance", rappId)).andExpect(status().isOk())
                .andExpect(
                        jsonPath("$." + rappInstanceId.toString() + ".rappInstanceId").value(rappInstanceId.toString()))
                .andExpect(jsonPath("$." + rappInstanceId.toString() + ".state").value(
                        RappInstanceState.UNDEPLOYED.name()));
    }

    @Test
    void testGetAllRappInstancesFailure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}/instance", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateRappInstance() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setRappInstanceId(rappInstanceId);
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance("test-instance");
        rappInstance.setAcm(rappACMInstance);
        rappInstance.setState(RappInstanceState.UNDEPLOYED);
        RappResources rappResources = new RappResources();
        rappResources.setAcm(RappResources.ACMResources.builder().compositionInstances(Set.of("test-instance")).build());
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).rappResources(rappResources).state(RappState.PRIMED).build();
        rappCacheService.putRapp(rapp);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/rapps/{rapp_id}/instance", rappId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rappInstance))).andExpect(status().isOk());
        Rapp rappResult = rappCacheService.getRapp(String.valueOf(rappId)).get();
        assertNotNull(rappResult.getRappInstances().get(rappInstanceId));
    }

    @Test
    void testCreateRappInstanceFailure() throws Exception {
        RappInstance rappInstance = new RappInstance();
        rappInstance.setRappInstanceId(UUID.randomUUID());
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance("test-instance");
        rappInstance.setAcm(rappACMInstance);
        rappInstance.setState(RappInstanceState.UNDEPLOYED);
        mockMvc.perform(MockMvcRequestBuilders.post("/rapps/{rapp_id}/instance", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappInstance)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetRappInstance() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rappInstanceId").value(rappInstanceId.toString()))
                .andExpect(jsonPath("$.state").value(RappInstanceState.UNDEPLOYED.name()));
    }

    @Test
    void testGetRappInstanceNoRappFailure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}/instance/{instance_id}", UUID.randomUUID(),
                UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void testGetRappInstanceNoRappInstanceFailure() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        rappCacheService.putRapp(rapp);
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "{}", "{\"acm\":\"\"}", "{\"sme\":\"\"}", "{\"dme\":\"\"}", "{\"acm\":{}}",
            "{\"sme\":{}}", "{\"dme\":{}}", "{\"acm\":{\"instance\":\"\"}}", "{\"sme\":{\"providerFunction\":\"\"}}",
            "{\"dme\":{\"infoTypesProducer\":\"\"}}"})
    void testCreateRappInstanceInvalidPayload(String payload) throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        rappCacheService.putRapp(rapp);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/rapps/{rapp_id}/instance", rappId).contentType(MediaType.APPLICATION_JSON)
                        .content(payload)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "asdasd", "non-uuid-string"})
    void testGetRappInstanceInvalidInstanceParameter(String rAppInstanceId) throws Exception {
        UUID rappId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}/instance/{instance_id}", rappId, rAppInstanceId))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testDeployRappInstance() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        RappInstanceDeployOrder rappInstanceDeployOrder = new RappInstanceDeployOrder();
        rappInstanceDeployOrder.setDeployOrder(DeployOrder.DEPLOY);
        when(acmDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappInstanceDeployOrder)))
                .andExpect(status().isAccepted());
    }


    @Test
    void testDeployNoRappInstanceFailure() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rapp.setRappInstances(Map.of());
        rappCacheService.putRapp(rapp);
        RappInstanceDeployOrder rappInstanceDeployOrder = new RappInstanceDeployOrder();
        rappInstanceDeployOrder.setDeployOrder(DeployOrder.DEPLOY);
        when(acmDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappInstanceDeployOrder)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeployNoRappFailure() throws Exception {
        RappInstanceDeployOrder rappInstanceDeployOrder = new RappInstanceDeployOrder();
        rappInstanceDeployOrder.setDeployOrder(DeployOrder.DEPLOY);
        when(acmDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}/instance/{instance_id}", UUID.randomUUID(),
                                UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappInstanceDeployOrder)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "{}", "{asdasd}", "{ \"deployOrder\": \"INVALID\"}", "{ \"deployOrder\": \"\"}"})
    void testDeployRappInstanceInvalidPayload(String payload) throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        RappInstanceDeployOrder rappInstanceDeployOrder = new RappInstanceDeployOrder();
        rappInstanceDeployOrder.setDeployOrder(DeployOrder.DEPLOY);
        when(acmDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId)
                                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "asdasd", "non-uuid-string"})
    void testDeployRappInstanceInvalidInstanceParameter(String rAppInstanceId) throws Exception {
        UUID rappId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}/instance/{instance_id}", rappId, rAppInstanceId)
                                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testUndeployRappInstance() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rapp.getRappInstances().forEach((uuid, rappInstance) -> rappInstance.setState(RappInstanceState.DEPLOYED));
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        RappInstanceDeployOrder rappInstanceDeployOrder = new RappInstanceDeployOrder();
        rappInstanceDeployOrder.setDeployOrder(DeployOrder.UNDEPLOY);
        when(acmDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappInstanceDeployOrder)))
                .andExpect(status().isAccepted());
    }

    @Test
    void testUndeployNoRappInstanceFailure() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rapp.setRappInstances(Map.of());
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        RappInstanceDeployOrder rappInstanceDeployOrder = new RappInstanceDeployOrder();
        rappInstanceDeployOrder.setDeployOrder(DeployOrder.UNDEPLOY);
        when(acmDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappInstanceDeployOrder)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUndeployNoRappFailure() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        RappInstanceDeployOrder rappInstanceDeployOrder = new RappInstanceDeployOrder();
        rappInstanceDeployOrder.setDeployOrder(DeployOrder.UNDEPLOY);
        when(acmDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappInstanceDeployOrder)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteRappInstance() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteRappNoRappFailure() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteRappNoInstanceFailure() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID rappInstanceId = UUID.randomUUID();
        Rapp rapp = getRapp(rappId, rappInstanceId);
        rapp.setRappInstances(Map.of());
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/rapps/{rapp_id}/instance/{instance_id}", rappId, rappInstanceId))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "asdasd", "non-uuid-string"})
    void testDeleteRappInstanceInvalidInstanceParameter(String rAppInstanceId) throws Exception {
        UUID rappId = UUID.randomUUID();
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/rapps/{rapp_id}/instance/{instance_id}", rappId, rAppInstanceId))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    Rapp getRapp(UUID rappId, UUID rappInstanceId) {
        RappInstance rappInstance = new RappInstance();
        rappInstance.setRappInstanceId(rappInstanceId);
        rappInstance.setState(RappInstanceState.UNDEPLOYED);
        Map<UUID, RappInstance> instances = new HashMap();
        instances.put(rappInstanceId, rappInstance);
        return Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                       .packageLocation(validCsarFileLocation).state(RappState.PRIMED).rappInstances(instances).build();
    }

}
