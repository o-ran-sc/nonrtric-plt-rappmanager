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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@ContextConfiguration(classes = {NamingValidator.class})
class NamingValidatorTest {

    @Autowired
    NamingValidator namingValidator;
    String validCsarFileLocation = "src/test/resources/";
    String validRappFile = "valid-rapp-package.csar";
    String expectedErrorMessage = "rApp package name should end with .csar";

    @Test
    void testNamingValidationSuccess() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertDoesNotThrow(() -> namingValidator.validate(multipartFile, null));
    }

    @Test
    void testNamingValidationFailureWithNull() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> namingValidator.validate(multipartFile, null));
        assertEquals(HttpStatus.BAD_REQUEST, rappValidationException.getStatusCode());
        assertEquals(expectedErrorMessage, rappValidationException.getMessage());
    }

    @Test
    void testNamingValidationFailureWithEmptyName() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("");
        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> namingValidator.validate(multipartFile, null));
        assertEquals(HttpStatus.BAD_REQUEST, rappValidationException.getStatusCode());
        assertEquals(expectedErrorMessage, rappValidationException.getMessage());
    }
}
