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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.oransc.rappmanager.models.BeanTestConfiguration;
import org.oransc.rappmanager.models.cache.RappCacheService;
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
@ContextConfiguration(classes = {BeanTestConfiguration.class, RappValidationHandler.class, NamingValidator.class,
        FileExistenceValidator.class, ArtifactDefinitionValidator.class, AsdDescriptorValidator.class,
        RappValidationUtils.class, RappsEnvironmentConfiguration.class, RappCsarConfigurationHandler.class,
        ObjectMapper.class, RappCacheService.class})
class RappValidationHandlerTest {

    @Autowired
    RappValidationHandler rappValidationHandler;
    String validCsarFileLocation = "src/test/resources/";
    private final String validRappFile = "valid-rapp-package.csar";

    @Test
    void testCsarPackageValidationSuccess() throws IOException {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertDoesNotThrow(() -> rappValidationHandler.isValidRappPackage(multipartFile));
    }

    @ParameterizedTest
    @MethodSource("getInvalidCsarPackage")
    void testCsarPackageValidationFailure(MultipartFile multipartFile, String errorMessage) {
        RappValidationException exception = assertThrows(RappValidationException.class,
                () -> rappValidationHandler.isValidRappPackage(multipartFile));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(errorMessage, exception.getMessage());
    }

    private static Stream<Arguments> getInvalidCsarPackage() throws IOException {
        String validCsarFileLocation = "src/test/resources";
        String errorMsgMissingAcmComposition = "rApp package missing a file Files/Acm/definition/compositions.json";
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
        return Stream.of(Arguments.of(multipartFile, errorMsgMissingAcmComposition),
                Arguments.of(multipartFileNoTosca, "rApp package missing a file TOSCA-Metadata/TOSCA.meta"),
                Arguments.of(multipartFileNoAsdYaml, "rApp package missing a file Definitions/asd.yaml"),
                Arguments.of(multipartFileMissingArtifact,
                        "rApp package missing a file Artifacts/Deployment/HELM/hello-world-chart-0.1.0.tgz"),
                Arguments.of(multipartFileNoComposition, errorMsgMissingAcmComposition));
    }

    @Test
    void testCsarPackageValidationFailureWithoutOrginalName() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        RappValidationException exception = assertThrows(RappValidationException.class,
                () -> rappValidationHandler.isValidRappPackage(multipartFile));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("rApp package name should end with .csar", exception.getMessage());
    }
}
