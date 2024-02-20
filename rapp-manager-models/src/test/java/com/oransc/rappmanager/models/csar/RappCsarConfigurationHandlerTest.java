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

package com.oransc.rappmanager.models.csar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappResources;
import com.oransc.rappmanager.models.rappinstance.RappACMInstance;
import com.oransc.rappmanager.models.rappinstance.RappDMEInstance;
import com.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {ObjectMapper.class, RappCsarConfigurationHandler.class})
class RappCsarConfigurationHandlerTest {

    @SpyBean
    RappCsarConfigurationHandler rappCsarConfigurationHandler;

    String validCsarFileLocation = "src/test/resources/";

    private final String validRappFile = "valid-rapp-package.csar";

    private final String invalidRappNoAsdFile = "invalid-rapp-package-no-asd-yaml.csar";

    private final String invalidRappEmptyAsdFile = "invalid-rapp-package-empty-asd-yaml.csar";

    @Test
    void testCsarInstantiationPayload() throws JSONException {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        UUID compositionId = UUID.randomUUID();
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance("kserve-instance");
        JSONObject jsonObject = new JSONObject(
                rappCsarConfigurationHandler.getInstantiationPayload(rapp, rappACMInstance, compositionId));
        assertEquals(jsonObject.get("compositionId"), String.valueOf(compositionId));
    }

    @Test
    void testFileListing() {
        File file = new File(validCsarFileLocation + validRappFile);
        Set<String> fileListFromCsar = rappCsarConfigurationHandler.getFileListFromCsar(file, "Files/Sme/serviceapis/");
        assertThat(fileListFromCsar).hasSize(2);
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
    void testListResources() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(rapp);
        assertThat(rappResources).isNotNull();
        assertNotNull(rappResources.getAcm().getCompositionDefinitions());
        assertThat(rappResources.getAcm().getCompositionInstances()).hasSize(4);
        assertThat(rappResources.getSme().getProviderFunctions()).hasSize(4);
        assertThat(rappResources.getSme().getServiceApis()).hasSize(2);
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
    void testGetSmeInvokerPayload() {
        UUID rappId = UUID.randomUUID();
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setServiceApis("invoker-app1");
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        String smeProviderDomainPayload = rappCsarConfigurationHandler.getSmeInvokerPayload(rapp, rappSMEInstance);
        assertNotNull(smeProviderDomainPayload);
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
    void testListDeploymentItems() {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        List<DeploymentItem> deploymentItems = rappCsarConfigurationHandler.getDeploymentItems(rapp);
        assertEquals(2, deploymentItems.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {invalidRappNoAsdFile, invalidRappEmptyAsdFile})
    void testListDeploymentItemsNoAsd(String packageName) {
        Rapp rapp = Rapp.builder().name("").packageName(packageName).packageLocation(validCsarFileLocation).build();
        assertThat(rappCsarConfigurationHandler.getDeploymentItems(rapp)).isEmpty();
    }

    @Test
    void testListDeploymentItemsWithException() throws JsonProcessingException {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        doThrow(new JsonSyntaxException("")).when(rappCsarConfigurationHandler).getAsdContentNode(any());
        assertThat(rappCsarConfigurationHandler.getDeploymentItems(rapp)).isEmpty();
    }

    @Test
    void testGetArtifactPayload() {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile).packageLocation(validCsarFileLocation).build();
        assertNotNull(rappCsarConfigurationHandler.getArtifactPayload(rapp,
                "Artifacts/Deployment/HELM/ransliceassurance-1.0.0.tgz"));

    }
}
