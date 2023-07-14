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

package com.oransc.rappmanager.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oransc.rappmanager.acm.service.AcmDeployer;
import com.oransc.rappmanager.configuration.RappManagerConfiguration;
import com.oransc.rappmanager.models.RappState;
import com.oransc.rappmanager.sme.service.SmeDeployer;
import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "rappmanager.csarlocation=src/test/resources")
@AutoConfigureMockMvc
public class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    RappManagerConfiguration rappManagerConfiguration;

    @MockBean
    AcmDeployer acmDeployer;

    @MockBean
    SmeDeployer smeDeployer;

    private final String validRappFile = "valid-rapp-package.csar";

    private final String invalidRappFile = "invalid-rapp-package.csar";

    @Test
    void testOnboardCsarPackage() throws Exception {
        String rappCsarPath = rappManagerConfiguration.getCsarLocation() + File.separator + validRappFile;
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", validRappFile, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        mockMvc.perform(
                        MockMvcRequestBuilders.multipart("/rapps/{rapp_id}/onboard", UUID.randomUUID()).file(multipartFile))
                .andExpect(status().isAccepted());
    }

    @Test
    void testOnboardCsarPackageFailure() throws Exception {
        String rappCsarPath = rappManagerConfiguration.getCsarLocation() + File.separator + invalidRappFile;
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", invalidRappFile, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        mockMvc.perform(
                        MockMvcRequestBuilders.multipart("/rapps/{rapp_id}/onboard", UUID.randomUUID()).file(multipartFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        this.onBoardRappCsar(rappId);
        this.mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}", rappId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(rappId.toString()))
                .andExpect(jsonPath("$.state").value(RappState.ONBOARDED.name()));
    }

    @Test
    void testGetInvalidRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        this.mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}", rappId)).andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRappDeploy() throws Exception {
        UUID rappId = UUID.randomUUID();
        this.onBoardRappCsar(rappId);
        when(acmDeployer.deployRapp(any())).thenReturn(true);
        when(smeDeployer.deployRapp(any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}/deploy", rappId))
                .andExpect(status().isAccepted());
    }

    @Test
    void testInvalidRappDeploy() throws Exception {
        UUID rappId = UUID.randomUUID();
        when(acmDeployer.deployRapp(any())).thenReturn(false);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}/deploy", rappId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testRappUndeploy() throws Exception {
        UUID rappId = UUID.randomUUID();
        this.onBoardRappCsar(rappId);
        when(acmDeployer.undeployRapp(any())).thenReturn(true);
        when(smeDeployer.undeployRapp(any())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}/undeploy", rappId))
                .andExpect(status().isAccepted());
    }

    @Test
    void testInvalidRappUndeploy() throws Exception {
        UUID rappId = UUID.randomUUID();
        when(acmDeployer.undeployRapp(any())).thenReturn(false);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}/undeploy", rappId))
                .andExpect(status().isInternalServerError());
    }

    void onBoardRappCsar(UUID rappId) throws Exception {
        String rappCsarPath = rappManagerConfiguration.getCsarLocation() + File.separator + validRappFile;
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", validRappFile, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}/onboard", rappId).file(multipartFile))
                .andExpect(status().isAccepted());
    }

}
