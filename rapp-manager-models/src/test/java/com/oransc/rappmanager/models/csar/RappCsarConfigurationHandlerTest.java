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

package com.oransc.rappmanager.models.csar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappResources;
import com.oransc.rappmanager.models.rappinstance.RappACMInstance;
import com.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@ContextConfiguration(classes = RappCsarConfigurationHandler.class)
class RappCsarConfigurationHandlerTest {

    @Autowired
    RappCsarConfigurationHandler rappCsarConfigurationHandler;

    String validCsarFileLocation = "src/test/resources/";


    private final String validRappFile = "valid-rapp-package.csar";

    private final String invalidRappFile = "invalid-rapp-package.csar";

    @Test
    void testCsarPackageValidationSuccess() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertEquals(Boolean.TRUE, rappCsarConfigurationHandler.isValidRappPackage(multipartFile));
    }

    @Test
    void testCsarPackageValidationFailure() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + invalidRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertEquals(Boolean.FALSE, rappCsarConfigurationHandler.isValidRappPackage(multipartFile));
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
    void testListResources() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .build();
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(rapp);
        assertThat(rappResources).isNotNull();
        assertNotNull(rappResources.getAcm().getCompositionDefinitions());
        assertThat(rappResources.getAcm().getCompositionInstances()).hasSize(3);
        assertThat(rappResources.getSme().getProviderFunctions()).hasSize(4);
        assertThat(rappResources.getSme().getServiceApis()).hasSize(2);
        assertThat(rappResources.getSme().getInvokers()).hasSize(2);
    }

    @Test
    void testListInvalidResources() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name("").build();
        RappResources rappResources = rappCsarConfigurationHandler.getRappResource(rapp);
        assertThat(rappResources).isNotNull();
        assertNull(rappResources.getAcm());
        assertNull(rappResources.getSme());
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

}
