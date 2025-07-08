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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.oransc.rappmanager.models.configuration.RappsEnvironmentConfiguration;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@ContextConfiguration(classes = {ArtifactDefinitionValidator.class, RappValidationUtils.class, ObjectMapper.class,
        RappsEnvironmentConfiguration.class, RappCsarConfigurationHandler.class})
class ArtifactDefinitionValidatorTest {

    @Autowired
    ArtifactDefinitionValidator artifactDefinitionValidator;
    @MockitoSpyBean
    RappValidationUtils rappValidationUtils;
    String validCsarFileLocation = "src/test/resources/";
    String validRappFile = "valid-rapp-package.csar";
    String invalidRappMissingArtifactFile = "invalid-rapp-package-missing-artifact.csar";

    @Test
    void testCsarContainsValidAsdFile() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertDoesNotThrow(() -> artifactDefinitionValidator.validate(multipartFile, null));
    }

    @Test
    void testCsarContainsValidAsdFileFailure() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + invalidRappMissingArtifactFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        RappValidationException exception = assertThrows(RappValidationException.class,
                () -> artifactDefinitionValidator.validate(multipartFile, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().startsWith("rApp package missing a file"));
    }


    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"{asasdasd"})
    void testCsarAsdContentInvalidFailure(String asdContent) throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        if (asdContent != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(asdContent.getBytes(), 0, asdContent.getBytes().length);
            doCallRealMethod().doReturn(byteArrayOutputStream).when(rappValidationUtils)
                    .getFileFromCsar(any(MultipartFile.class), any());
        } else {
            doCallRealMethod().doReturn(asdContent).when(rappValidationUtils)
                    .getFileFromCsar(any(MultipartFile.class), any());
        }
        RappValidationException exception = assertThrows(RappValidationException.class,
                () -> artifactDefinitionValidator.validate(multipartFile, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("ASD definition in rApp package is invalid.", exception.getMessage());
    }
}
