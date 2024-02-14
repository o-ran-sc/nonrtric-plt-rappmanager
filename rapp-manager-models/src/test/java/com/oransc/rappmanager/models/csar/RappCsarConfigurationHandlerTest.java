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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.oransc.rappmanager.models.exception.RappHandlerException;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappResources;
import com.oransc.rappmanager.models.rappinstance.RappACMInstance;
import com.oransc.rappmanager.models.rappinstance.RappDMEInstance;
import com.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@ContextConfiguration(classes = {ObjectMapper.class, RappCsarConfigurationHandler.class})
class RappCsarConfigurationHandlerTest {

    @SpyBean
    RappCsarConfigurationHandler rappCsarConfigurationHandler;

    String validCsarFileLocation = "src/test/resources/";

    private final String validRappFile = "valid-rapp-package.csar";

    private final String invalidRappFile = "invalid-rapp-package.csar";

    private final String invalidRappNoAsdFile = "invalid-rapp-package-no-asd-yaml.csar";

    private final String invalidRappEmptyAsdFile = "invalid-rapp-package-empty-asd-yaml.csar";

    @Test
    void testCsarPackageValidationSuccess() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertEquals(Boolean.TRUE, rappCsarConfigurationHandler.isValidRappPackage(multipartFile));
    }

    @ParameterizedTest
    @MethodSource("getInvalidCsarPackage")
    void testCsarPackageValidationFailure(MultipartFile multipartFile) {
        RappHandlerException exception = assertThrows(RappHandlerException.class,
                () -> rappCsarConfigurationHandler.isValidRappPackage(multipartFile));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }


    private static Stream<Arguments> getInvalidCsarPackage() throws IOException {
        String validCsarFileLocation = "src/test/resources";
        String rappCsarPath = validCsarFileLocation + File.separator + "invalid-rapp-package.csar";
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        String rappCsarPathNoTosca = validCsarFileLocation + File.separator + "invalid-rapp-package-no-tosca.csar";
        MultipartFile multipartFileNoTosca = new MockMultipartFile(rappCsarPathNoTosca, rappCsarPathNoTosca,
                ContentType.MULTIPART_FORM_DATA.getMimeType(), new FileInputStream(rappCsarPathNoTosca));
        String rappCsarPathNoAsdYaml = validCsarFileLocation + File.separator + "invalid-rapp-package-no-asd-yaml.csar";
        MultipartFile multipartFileNoAsdYaml = new MockMultipartFile(rappCsarPathNoAsdYaml, rappCsarPathNoAsdYaml,
                ContentType.MULTIPART_FORM_DATA.getMimeType(), new FileInputStream(rappCsarPathNoAsdYaml));
        String rappCsarPathMissingArtifact =
                validCsarFileLocation + File.separator + "invalid-rapp-package-missing-artifact.csar";
        MultipartFile multipartFileMissingArtifact =
                new MockMultipartFile(rappCsarPathMissingArtifact, rappCsarPathMissingArtifact,
                        ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPathMissingArtifact));
        String rappCsarPathNoComposition =
                validCsarFileLocation + File.separator + "invalid-rapp-package-no-acm-composition.csar";
        MultipartFile multipartFileNoComposition =
                new MockMultipartFile(rappCsarPathNoComposition, rappCsarPathNoComposition,
                        ContentType.MULTIPART_FORM_DATA.getMimeType(), new FileInputStream(rappCsarPathNoComposition));
        return Stream.of(Arguments.of(multipartFile), Arguments.of(multipartFileNoTosca),
                Arguments.of(multipartFileNoAsdYaml), Arguments.of(multipartFileMissingArtifact),
                Arguments.of(multipartFileNoComposition));
    }

    @Test
    void testCsarPackageValidationFailureWithoutOrginalName() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        assertEquals(Boolean.FALSE, rappCsarConfigurationHandler.isValidRappPackage(multipartFile));
    }

    @Test
    void testInvalidCsarFileExist() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        RappHandlerException exception = assertThrows(RappHandlerException.class,
                () -> rappCsarConfigurationHandler.isFileExistsInCsar(multipartFile, "INVALID_LOCATION"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testCsarContainsValidAsdFile() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertTrue(rappCsarConfigurationHandler.containsValidArtifactDefinition(multipartFile));
    }

    @Test
    void testCsarContainsValidAsdFileFailure() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + invalidRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        RappHandlerException exception = assertThrows(RappHandlerException.class,
                () -> rappCsarConfigurationHandler.containsValidArtifactDefinition(multipartFile));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testCsarNoAsdFailure() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        RappHandlerException exception = assertThrows(RappHandlerException.class,
                () -> rappCsarConfigurationHandler.containsValidArtifactDefinition(multipartFile));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testCsarAsdContentInvalidFailure() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        String invalidJson = "{asasdasd";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(invalidJson.getBytes(), 0, invalidJson.getBytes().length);
        doCallRealMethod().doReturn(byteArrayOutputStream).when(rappCsarConfigurationHandler)
                .getFileFromCsar(any(MultipartFile.class), any());
        RappHandlerException exception = assertThrows(RappHandlerException.class,
                () -> rappCsarConfigurationHandler.containsValidArtifactDefinition(multipartFile));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testgetFileFromCsarFailure() throws IOException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getInputStream()).thenThrow(new IOException());
        assertThat(rappCsarConfigurationHandler.getFileFromCsar(multipartFile, null).size()).isZero();
    }

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
