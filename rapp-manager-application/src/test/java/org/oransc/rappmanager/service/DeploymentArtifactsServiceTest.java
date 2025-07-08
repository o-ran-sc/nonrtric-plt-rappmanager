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

package org.oransc.rappmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.oransc.rappmanager.models.csar.DeploymentItem;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.exception.RappHandlerException;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappState;
import org.oransc.rappmanager.sme.service.SmeLifecycleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeploymentArtifactsServiceTest {

    @MockitoSpyBean
    DeploymentArtifactsService deploymentArtifactsService;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    RappCsarConfigurationHandler rappCsarConfigurationHandler;
    @MockitoBean
    SmeLifecycleManager smeLifecycleManager;

    MockRestServiceServer mockServer;

    String validCsarFileLocation = "src/test/resources/";

    private final String validRappFile = "valid-rapp-package.csar";

    @BeforeEach
    void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"CREATED", "CONFLICT"})
    void testChartUpload(HttpStatus status) {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rapp.setAsdMetadata(rappCsarConfigurationHandler.getAsdMetadata(rapp));
        List<DeploymentItem> deploymentItems = rapp.getAsdMetadata().getDeploymentItems();
        deploymentItems.forEach(deploymentItem -> mockServer.expect(ExpectedCount.once(),
                        requestTo(deploymentItem.getTargetServerUri())).andExpect(method(HttpMethod.POST))
                                                          .andRespond(withStatus(status)));
        assertTrue(deploymentArtifactsService.configureDeploymentArtifacts(rapp));
        mockServer.verify();
    }

    @Test
    void testChartUploadNoArtifacts() {
        String invalidRappFile = "valid-rapp-package-no-artifacts.csar";
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(invalidRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rapp.setAsdMetadata(rappCsarConfigurationHandler.getAsdMetadata(rapp));
        assertTrue(deploymentArtifactsService.configureDeploymentArtifacts(rapp));
    }

    @Test
    void testChartUploadFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rapp.setAsdMetadata(rappCsarConfigurationHandler.getAsdMetadata(rapp));
        List<DeploymentItem> deploymentItems = rapp.getAsdMetadata().getDeploymentItems();
        deploymentItems.stream().findFirst().ifPresent(deploymentItem -> mockServer.expect(ExpectedCount.once(),
                        requestTo(deploymentItem.getTargetServerUri())).andExpect(method(HttpMethod.POST))
                                                                                 .andRespond(withServerError()));
        RappHandlerException exception = assertThrows(RappHandlerException.class,
                () -> deploymentArtifactsService.configureDeploymentArtifacts(rapp));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        mockServer.verify();
    }

    @Test
    void testChartUploadFailureWithNotFound() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rapp.setAsdMetadata(rappCsarConfigurationHandler.getAsdMetadata(rapp));
        List<DeploymentItem> deploymentItems = rapp.getAsdMetadata().getDeploymentItems();
        deploymentItems.stream().findFirst().ifPresent(deploymentItem -> mockServer.expect(ExpectedCount.once(),
                requestTo(deploymentItem.getTargetServerUri())).andExpect(method(HttpMethod.POST)).andRespond(
                withStatus(HttpStatus.NOT_FOUND)));
        assertFalse(deploymentArtifactsService.configureDeploymentArtifacts(rapp));
        mockServer.verify();
    }

    @Test
    void testChartUploadFailureWithException() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rapp.setAsdMetadata(rappCsarConfigurationHandler.getAsdMetadata(rapp));
        List<DeploymentItem> deploymentItems = rapp.getAsdMetadata().getDeploymentItems();
        deploymentItems.stream().findFirst().ifPresent(deploymentItem -> mockServer.expect(ExpectedCount.once(),
                requestTo(deploymentItem.getTargetServerUri())).andExpect(method(HttpMethod.POST)).andRespond(
                withException(new IOException())));
        RappHandlerException exception = assertThrows(RappHandlerException.class,
                () -> deploymentArtifactsService.configureDeploymentArtifacts(rapp));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        mockServer.verify();
    }

    @Test
    void testChartUploadFailureWithTooManyRequests() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rapp.setAsdMetadata(rappCsarConfigurationHandler.getAsdMetadata(rapp));
        List<DeploymentItem> deploymentItems = rapp.getAsdMetadata().getDeploymentItems();
        deploymentItems.stream().findFirst().ifPresent(deploymentItem -> mockServer.expect(ExpectedCount.once(),
                        requestTo(deploymentItem.getTargetServerUri())).andExpect(method(HttpMethod.POST))
                                                                                 .andRespond(withTooManyRequests()));
        assertFalse(deploymentArtifactsService.configureDeploymentArtifacts(rapp));
        mockServer.verify();
    }
}
