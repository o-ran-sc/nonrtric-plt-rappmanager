/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.models.csar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.oransc.rappmanager.models.configuration.RappsEnvironmentConfiguration;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappResources;
import org.oransc.rappmanager.models.rappinstance.RappACMInstance;
import org.oransc.rappmanager.models.rappinstance.RappDMEInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(
        classes = {ObjectMapper.class, RappsEnvironmentConfiguration.class, RappCsarConfigurationHandler.class})
class RappCsarConfigurationHandlerTest {

    @MockitoSpyBean
    RappCsarConfigurationHandler rappCsarConfigurationHandler;
    @Autowired
    RappsEnvironmentConfiguration rappsEnvironmentConfiguration;
    @Autowired
    ObjectMapper objectMapper;

    String validCsarFileLocation = "src/test/resources/";

    private final String validRappFile = "valid-rapp-package.csar";

    private final String invalidRappNoAsdFile = "invalid-rapp-package-no-asd-yaml.csar";

    private final String invalidRappEmptyAsdFile = "invalid-rapp-package-empty-asd-yaml.csar";

    @Test
    void testCsarInstantiationPayload() throws JSONException, JsonProcessingException {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        UUID compositionId = UUID.randomUUID();
        RappInstance rappInstance = new RappInstance();
        RappACMInstance rappKserveACMInstance = new RappACMInstance();
        rappKserveACMInstance.setInstance("kserve-instance");
        rappInstance.setAcm(rappKserveACMInstance);
        JSONObject kserveInstanceJsonObject =
                new JSONObject(rappCsarConfigurationHandler.getInstantiationPayload(rapp, rappInstance, compositionId));
        assertEquals(kserveInstanceJsonObject.get("compositionId"), String.valueOf(compositionId));
        RappACMInstance rappK8sACMInstance = new RappACMInstance();
        rappK8sACMInstance.setInstance("k8s-instance");
        rappInstance.setAcm(rappK8sACMInstance);
        JsonNode jsonNode = objectMapper.readTree(
                rappCsarConfigurationHandler.getInstantiationPayload(rapp, rappInstance, compositionId));
        assertEquals(jsonNode.get("compositionId").asText(), String.valueOf(compositionId));
        JsonNode overrideParamsNode = jsonNode.at("/elements").elements().next().at("/properties/chart/overrideParams");
        System.out.println(overrideParamsNode);
        assertEquals(overrideParamsNode.get("environment.appId").asText(), rappInstance.getRappInstanceId().toString());
        assertEquals(overrideParamsNode.get("environment.smeDiscoveryEndpoint").asText(),
                rappsEnvironmentConfiguration.getSmeDiscoveryEndpoint());
    }

    @Test
    void testFileListing() {
        File file = new File(validCsarFileLocation + validRappFile);
        Set<String> fileListFromCsar = rappCsarConfigurationHandler.getFileListFromCsar(file, "Files/Sme/serviceapis/");
        assertThat(fileListFromCsar).hasSize(3);
    }

    @Test
    void testInvalidFileListing() {
        File file = new File(validCsarFileLocation);
        Set<String> fileListFromCsar = rappCsarConfigurationHandler.getFileListFromCsar(file, null);
        assertThat(fileListFromCsar).isEmpty();
    }

    @Test
    void testInvalidFileListingFromCsar() {
        File file = new File("InvalidFile");
        ByteArrayOutputStream fileByteArray = rappCsarConfigurationHandler.getFileFromCsar(file, null);
        assertThat(fileByteArray.size()).isZero();
    }

    @Test
    void testInvalidZipStreamGetFromCsar() throws IOException {
        ZipArchiveInputStream zipArchiveInputStream = mock(ZipArchiveInputStream.class);
        doThrow(new IOException()).when(zipArchiveInputStream).getNextEntry();
        ByteArrayOutputStream fileByteArray = rappCsarConfigurationHandler.getFileFromCsar(zipArchiveInputStream, null);
        assertThat(fileByteArray.size()).isZero();
    }

    @Test
    void testListResources() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(rapp);
        assertThat(rappResources).isNotNull();
        assertNotNull(rappResources.getAcm().getCompositionDefinitions());
        assertThat(rappResources.getAcm().getCompositionInstances()).hasSize(4);
        assertThat(rappResources.getSme().getProviderFunctions()).hasSize(3);
        assertThat(rappResources.getSme().getServiceApis()).hasSize(3);
        assertThat(rappResources.getSme().getInvokers()).hasSize(2);
        assertThat(rappResources.getDme().getProducerInfoTypes()).hasSize(2);
        assertThat(rappResources.getDme().getConsumerInfoTypes()).hasSize(2);
        assertThat(rappResources.getDme().getInfoProducers()).hasSize(2);
        assertThat(rappResources.getDme().getInfoConsumers()).hasSize(2);
    }

    @Test
    void testListInvalidResources() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name("").packageName("").packageLocation("").build();
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(rapp);
        assertThat(rappResources).isNotNull();
        assertNull(rappResources.getAcm());
        assertNull(rappResources.getSme());
        assertNull(rappResources.getDme());
    }

    @Test
    void testGetAcmCompositionPayload() {
        UUID rappId = UUID.randomUUID();
        RappResources rappResources = new RappResources();
        rappResources.setAcm(RappResources.ACMResources.builder().compositionDefinitions("compositions")
                                     .compositionInstances(Set.of()).build());
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .rappResources(rappResources).build();
        String acmCompositionPayload = rappCsarConfigurationHandler.getAcmCompositionPayload(rapp);
        assertNotNull(acmCompositionPayload);
    }

    @Test
    void testGetInvalidAcmCompositionPayload() {
        UUID rappId = UUID.randomUUID();
        RappResources rappResources = new RappResources();
        rappResources.setAcm(RappResources.ACMResources.builder().compositionDefinitions("invalidcomposition")
                                     .compositionInstances(Set.of()).build());
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .rappResources(rappResources).build();
        String acmCompositionPayload = rappCsarConfigurationHandler.getAcmCompositionPayload(rapp);
        assertEquals("", acmCompositionPayload);
    }

    @Test
    void testGetSmeProviderDomainPayload() {
        UUID rappId = UUID.randomUUID();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setProviderFunction("aef-provider-function");
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        String smeProviderDomainPayload =
                rappCsarConfigurationHandler.getSmeProviderDomainPayload(rapp, rappSMEInstance);
        assertNotNull(smeProviderDomainPayload);
    }

    @Test
    void testGetSmeServiceApiPayload() {
        UUID rappId = UUID.randomUUID();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setServiceApis("api-set-1");
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        String smeProviderDomainPayload = rappCsarConfigurationHandler.getSmeProviderApiPayload(rapp, rappSMEInstance);
        assertNotNull(smeProviderDomainPayload);
    }

    @Test
    void testGetSmeInvokerPayload() throws JSONException {
        UUID rappId = UUID.randomUUID();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setInvokers("invoker-app1");
        RappInstance rappInstance = new RappInstance();
        rappInstance.setSme(rappSMEInstance);
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        JSONArray smeInvokerPayload =
                new JSONArray(rappCsarConfigurationHandler.getSmeInvokerPayload(rapp, rappInstance));
        assertNotNull(smeInvokerPayload);
        assertEquals(smeInvokerPayload.getJSONObject(0).getString("apiInvokerInformation"),
                rappInstance.getRappInstanceId().toString());
    }

    @Test
    void testGetDmeProducerInfoTypePayload() {
        UUID rappId = UUID.randomUUID();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of("json-file-data-from-filestore"));
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        String dmeInfoTypePayload = rappCsarConfigurationHandler.getDmeProducerInfoTypePayload(rapp,
                rappDMEInstance.getInfoTypesProducer().iterator().next());
        assertNotNull(dmeInfoTypePayload);
    }

    @Test
    void testGetDmeConsumerInfoTypePayload() {
        UUID rappId = UUID.randomUUID();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypeConsumer("json-file-data-from-filestore");
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        String dmeInfoTypePayload =
                rappCsarConfigurationHandler.getDmeConsumerInfoTypePayload(rapp, rappDMEInstance.getInfoTypeConsumer());
        assertNotNull(dmeInfoTypePayload);
    }

    @Test
    void testGetDmeInfoProducerPayload() {
        UUID rappId = UUID.randomUUID();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoProducer("json-file-data-producer");
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        String dmeInfoProducerPayload =
                rappCsarConfigurationHandler.getDmeInfoProducerPayload(rapp, rappDMEInstance.getInfoProducer());
        assertNotNull(dmeInfoProducerPayload);
    }

    @Test
    void testGetDmeInfoConsumerPayload() {
        UUID rappId = UUID.randomUUID();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoConsumer("json-file-consumer");
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        String dmeInfoConsumerPayload =
                rappCsarConfigurationHandler.getDmeInfoConsumerPayload(rapp, rappDMEInstance.getInfoConsumer());
        assertNotNull(dmeInfoConsumerPayload);
    }

    @Test
    void testGetAsdMetadata() {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        AsdMetadata asdMetadata = rappCsarConfigurationHandler.getAsdMetadata(rapp);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", asdMetadata.getDescriptorId());
        assertEquals("040eff2a-eb1a-4aff-bd46-37ce38092985", asdMetadata.getDescriptorInvariantId());
        assertEquals(2, asdMetadata.getDeploymentItems().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {invalidRappNoAsdFile, invalidRappEmptyAsdFile})
    void testGetAsdMetadataNoAsd(String packageName) {
        Rapp rapp = Rapp.builder().name("").packageName(packageName).packageLocation(validCsarFileLocation).build();
        assertThat(rappCsarConfigurationHandler.getAsdMetadata(rapp)).isNotNull();
    }

    @Test
    void testGetAsdMetadataException() throws JsonProcessingException {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        doThrow(new JsonSyntaxException("")).when(rappCsarConfigurationHandler).getAsdContentNode(any());
        assertNull(rappCsarConfigurationHandler.getAsdMetadata(rapp).getDescriptorId());
        assertNull(rappCsarConfigurationHandler.getAsdMetadata(rapp).getDescriptorInvariantId());
        assertThat(rappCsarConfigurationHandler.getAsdMetadata(rapp).getDeploymentItems()).isNull();
    }

    @Test
    void testGetAsdMetadataNullAsdContent() throws JsonProcessingException {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        doReturn(null).when(rappCsarConfigurationHandler).getAsdContentNode(any());
        assertNull(rappCsarConfigurationHandler.getAsdMetadata(rapp).getDescriptorId());
        assertNull(rappCsarConfigurationHandler.getAsdMetadata(rapp).getDescriptorInvariantId());
        assertThat(rappCsarConfigurationHandler.getAsdMetadata(rapp).getDeploymentItems()).isNull();
    }

    @Test
    void testGetArtifactPayload() {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        assertNotNull(rappCsarConfigurationHandler.getArtifactPayload(rapp,
                "Artifacts/Deployment/HELM/ransliceassurance-1.0.0.tgz"));

    }
}
