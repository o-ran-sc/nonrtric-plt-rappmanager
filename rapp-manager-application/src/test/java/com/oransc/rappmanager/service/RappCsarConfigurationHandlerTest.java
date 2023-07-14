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

package com.oransc.rappmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.configuration.RappManagerConfiguration;

import com.oransc.rappmanager.models.Rapp;
import com.oransc.rappmanager.models.RappCsarConfigurationHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@TestPropertySource(properties = "rappmanager.csarlocation=src/test/resources")
public class RappCsarConfigurationHandlerTest {

    @Autowired
    RappCsarConfigurationHandler rappCsarConfigurationHandler;

    @Autowired
    RappManagerConfiguration rappManagerConfiguration;

    ObjectMapper objectMapper = new ObjectMapper();


    private final String validRappFile = "valid-rapp-package.csar";

    private final String invalidRappFile = "invalid-rapp-package.csar";

    @Test
    void testCsarPackageValidationSuccess() throws IOException {
        String rappCsarPath = rappManagerConfiguration.getCsarLocation() + File.separator + validRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertEquals(Boolean.TRUE, rappCsarConfigurationHandler.isValidRappPackage(multipartFile));
    }

    @Test
    void testCsarPackageValidationFailure() throws IOException {
        String rappCsarPath = rappManagerConfiguration.getCsarLocation() + File.separator + invalidRappFile;
        MultipartFile multipartFile =
                new MockMultipartFile(rappCsarPath, rappCsarPath, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        assertEquals(Boolean.FALSE, rappCsarConfigurationHandler.isValidRappPackage(multipartFile));
    }

    @Test
    void testCsarInstantiationPayload() throws JsonProcessingException {
        Rapp rapp = Rapp.builder().name("").packageName(validRappFile)
                            .packageLocation(rappManagerConfiguration.getCsarLocation()).build();
        UUID compositionId = UUID.randomUUID();
        AutomationComposition automationComposition =
                objectMapper.readValue(rappCsarConfigurationHandler.getInstantiationPayload(rapp, compositionId),
                        AutomationComposition.class);
        assertEquals(automationComposition.getCompositionId(), compositionId);
    }
}
