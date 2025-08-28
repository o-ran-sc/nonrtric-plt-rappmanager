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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.oransc.rappmanager.acm.service.AcmDeployer;
import org.oransc.rappmanager.dme.service.DmeDeployer;
import org.oransc.rappmanager.models.cache.RappCacheService;
import org.oransc.rappmanager.models.csar.AsdMetadata;
import org.oransc.rappmanager.models.rapp.PrimeOrder;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappPrimeOrder;
import org.oransc.rappmanager.models.rapp.RappState;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstanceState;
import org.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import org.oransc.rappmanager.sme.service.SmeLifecycleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RappControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RappCacheService rappCacheService;

    @Autowired
    RappInstanceStateMachine rappInstanceStateMachine;

    @MockitoBean
    AcmDeployer acmDeployer;

    @MockitoBean
    DmeDeployer dmeDeployer;

    @MockitoBean
    SmeLifecycleManager smeLifecycleManager;

    private final String validRappFile = "valid-rapp-package.csar";
    private final String validCsarFileLocation = "src/test/resources/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetRapps() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        RappInstance instance = new RappInstance();
        instance.setRappInstanceId(instanceId);
        instance.setState(RappInstanceState.UNDEPLOYED);
        Map<UUID, RappInstance> instances = Map.of(instanceId, instance);
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).rappInstances(instances).build();
        AsdMetadata asdMetadata = new AsdMetadata();
        asdMetadata.setDescriptorId(UUID.randomUUID().toString());
        asdMetadata.setDescriptorInvariantId(UUID.randomUUID().toString());
        asdMetadata.setDeploymentItems(List.of());
        rapp.setAsdMetadata(asdMetadata);
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(instanceId);
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rappInstances." + instanceId + ".rappInstanceId").value(instanceId.toString()))
                .andExpect(jsonPath("$[0].rappInstances." + instanceId + ".state").value(instance.getState().name()));
    }

    @Test
    void testGetRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        RappInstance instance = new RappInstance();
        instance.setRappInstanceId(instanceId);
        instance.setState(RappInstanceState.UNDEPLOYED);
        Map<UUID, RappInstance> instances = Map.of(instanceId, instance);
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).rappInstances(instances).build();
        AsdMetadata asdMetadata = new AsdMetadata();
        asdMetadata.setDescriptorId(UUID.randomUUID().toString());
        asdMetadata.setDescriptorInvariantId(UUID.randomUUID().toString());
        asdMetadata.setDeploymentItems(List.of());
        rapp.setAsdMetadata(asdMetadata);
        rappCacheService.putRapp(rapp);
        rappInstanceStateMachine.onboardRappInstance(instanceId);
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}", rappId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rappId").value(rappId.toString()))
                .andExpect(jsonPath("$.state").value(RappState.COMMISSIONED.name()))
                .andExpect(jsonPath("$.rappInstances." + instanceId + ".rappInstanceId").value(instanceId.toString()))
                .andExpect(jsonPath("$.rappInstances." + instanceId + ".state").value(instance.getState().name()));
    }

    @Test
    void testGetInvalidRapp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateRapp() throws Exception {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", validRappFile, MediaType.MULTIPART_FORM_DATA.getType(),
                        new FileInputStream(rappCsarPath));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}", UUID.randomUUID()).file(multipartFile))
                .andExpect(status().isAccepted());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-rapp-package.csar", "invalid-rapp-package-no-asd-yaml.csar",
            "invalid-rapp-package-empty-asd-yaml.csar", "invalid-rapp-package-no-tosca.csar",
            "invalid-rapp-package-no-acm-composition.csar", "invalid-rapp-package-missing-artifact.csar"})
    void testCreateInvalidRapp(String rAppPackageName) throws Exception {
        String rappCsarPath = validCsarFileLocation + File.separator + rAppPackageName;
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", rAppPackageName, MediaType.MULTIPART_FORM_DATA.getType(),
                        new FileInputStream(rappCsarPath));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}", UUID.randomUUID()).file(multipartFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateInvalidMultipartRapp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPrimeRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        AsdMetadata asdMetadata = new AsdMetadata();
        asdMetadata.setDescriptorId(UUID.randomUUID().toString());
        asdMetadata.setDescriptorInvariantId(UUID.randomUUID().toString());
        asdMetadata.setDeploymentItems(List.of());
        rapp.setAsdMetadata(asdMetadata);
        rappCacheService.putRapp(rapp);
        when(acmDeployer.primeRapp(any())).thenReturn(true);
        when(dmeDeployer.primeRapp(any())).thenReturn(true);
        RappPrimeOrder rappPrimeOrder = new RappPrimeOrder();
        rappPrimeOrder.setPrimeOrder(PrimeOrder.PRIME);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}", rappId).contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappPrimeOrder))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = PrimeOrder.class, names = {"PRIME", "DEPRIME"})
    void testPrimeRappFailure(PrimeOrder primeOrder) throws Exception {
        RappPrimeOrder rappPrimeOrder = new RappPrimeOrder();
        rappPrimeOrder.setPrimeOrder(primeOrder);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappPrimeOrder)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "{}", "{asdasd}", "{ \"primeOrder\": \"INVALID\"}", "{ \"primeOrder\": \"\"}"})
    void testPrimeRappInvalidPayload(String payload) throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        AsdMetadata asdMetadata = new AsdMetadata();
        asdMetadata.setDescriptorId(UUID.randomUUID().toString());
        asdMetadata.setDescriptorInvariantId(UUID.randomUUID().toString());
        asdMetadata.setDeploymentItems(List.of());
        rapp.setAsdMetadata(asdMetadata);
        rappCacheService.putRapp(rapp);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}", rappId).contentType(MediaType.APPLICATION_JSON)
                                .content(payload)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testDeprimeRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        AsdMetadata asdMetadata = new AsdMetadata();
        asdMetadata.setDescriptorId(UUID.randomUUID().toString());
        asdMetadata.setDescriptorInvariantId(UUID.randomUUID().toString());
        asdMetadata.setDeploymentItems(List.of());
        rapp.setAsdMetadata(asdMetadata);
        rappCacheService.putRapp(rapp);
        when(acmDeployer.deprimeRapp(any())).thenReturn(true);
        when(dmeDeployer.deprimeRapp(any())).thenReturn(true);
        RappPrimeOrder rappPrimeOrder = new RappPrimeOrder();
        rappPrimeOrder.setPrimeOrder(PrimeOrder.DEPRIME);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}", rappId).contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappPrimeOrder))).andExpect(status().isOk());
    }

    @Test
    void testDeleteRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        AsdMetadata asdMetadata = new AsdMetadata();
        asdMetadata.setDescriptorId(UUID.randomUUID().toString());
        asdMetadata.setDescriptorInvariantId(UUID.randomUUID().toString());
        asdMetadata.setDeploymentItems(List.of());
        rapp.setAsdMetadata(asdMetadata);
        rappCacheService.putRapp(rapp);
        mockMvc.perform(MockMvcRequestBuilders.delete("/rapps/{rapp_id}", rappId)).andExpect(status().isOk());
    }

    @Test
    void testDeleteRappFailure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/rapps/{rapp_id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
