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


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.models.configuration.RappsEnvironmentConfiguration;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@ContextConfiguration(classes = {FileExistenceValidator.class, RappValidationUtils.class, ObjectMapper.class,
        RappsEnvironmentConfiguration.class, RappCsarConfigurationHandler.class})
class FileExistenceValidatorTest {

    @Autowired
    FileExistenceValidator fileExistenceValidator;
    String validCsarFileLocation = "src/test/resources/";
    String validRappFile = "valid-rapp-package.csar";
    String invalidRappFile = "invalid-rapp-package.csar";
    String invalidRappFileNoTosca = "invalid-rapp-package-no-tosca.csar";

    @Test
    void testFileExistenceValidationSuccess() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertDoesNotThrow(() -> fileExistenceValidator.validate(multipartFile, null));
    }

    @Test
    void testFileExistenceNoCompositionValidation() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + invalidRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> fileExistenceValidator.validate(multipartFile, null));
        assertEquals(HttpStatus.BAD_REQUEST, rappValidationException.getStatusCode());
        assertEquals("rApp package missing a file Files/Acm/definition/compositions.json",
                rappValidationException.getMessage());
    }

    @Test
    void testFileExistenceNoToscaValidation() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + invalidRappFileNoTosca;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> fileExistenceValidator.validate(multipartFile, null));
        assertEquals(HttpStatus.BAD_REQUEST, rappValidationException.getStatusCode());
        assertEquals("rApp package missing a file TOSCA-Metadata/TOSCA.meta", rappValidationException.getMessage());
    }
}
