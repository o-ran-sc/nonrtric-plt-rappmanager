/*
 * ============LICENSE_START======================================================================
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
 *
 */

package org.oransc.rappmanager.models.csar.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.models.configuration.RappsEnvironmentConfiguration;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.csar.RappCsarPathProvider;
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@ContextConfiguration(classes = {RappValidationUtils.class, ObjectMapper.class, RappsEnvironmentConfiguration.class,
        RappCsarConfigurationHandler.class})
class RappValidationUtilsTest {

    String validCsarFileLocation = "src/test/resources/";
    private final String validRappFile = "valid-rapp-package.csar";
    private final String invalidRappNoToscaFile = "invalid-rapp-package-no-tosca.csar";
    @Autowired
    RappValidationUtils rappValidationUtils;

    @Test
    void testCsarFileExist() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertTrue(rappValidationUtils.isFileExistsInCsar(multipartFile, RappCsarPathProvider.TOSCA_METADATA_LOCATION));

    }

    @Test
    void testInvalidCsarFileExist() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        RappValidationException exception = assertThrows(RappValidationException.class,
                () -> rappValidationUtils.isFileExistsInCsar(multipartFile, "INVALID_LOCATION"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("rApp package missing a file INVALID_LOCATION", exception.getMessage());
    }

    @Test
    void testGetFileFromCsar() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertNotNull(rappValidationUtils.getFileFromCsar(multipartFile, RappCsarPathProvider.TOSCA_METADATA_LOCATION));
    }

    @Test
    void testGetFileFromCsarFailure() throws IOException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getInputStream()).thenThrow(new IOException());
        RappValidationException exception = assertThrows(RappValidationException.class,
                () -> rappValidationUtils.getFileFromCsar(multipartFile, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(String.format("Unable to get file %s from the multipart CSAR file", (Object) null),
                exception.getMessage());
    }

    @Test
    void testGetAsdLocationCsar() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertEquals("Definitions/asd.yaml", rappValidationUtils.getAsdDefinitionLocation(multipartFile));
    }

    @Test
    void testGetAsdLocationCsarFailure() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + invalidRappNoToscaFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertEquals("", rappValidationUtils.getAsdDefinitionLocation(multipartFile));
    }
}
