/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.acm.service.AcmDeployer;
import org.oransc.rappmanager.dme.service.DmeDeployer;
import org.oransc.rappmanager.models.exception.RappHandlerException;
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappState;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstanceState;
import org.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import org.oransc.rappmanager.sme.service.SmeDeployer;
import org.oransc.rappmanager.sme.service.SmeLifecycleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RappServiceTest {

    @Autowired
    RappService rappService;

    @MockitoBean
    AcmDeployer acmDeployer;

    @MockitoBean
    SmeDeployer smeDeployer;

    @MockitoBean
    DmeDeployer dmeDeployer;

    @MockitoBean
    DeploymentArtifactsService deploymentArtifactsService;

    @MockitoBean
    SmeLifecycleManager smeLifecycleManager;

    @Autowired
    RappInstanceStateMachine rappInstanceStateMachine;

    String validCsarFileLocation = "src/test/resources/";

    private final String validRappFile = "valid-rapp-package.csar";

    private final String stateTransitionNotPermitted = "State transition from %s to %s is not permitted.";


    @Test
    void testPrimeRapp() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        when(acmDeployer.primeRapp(any())).thenReturn(true);
        when(dmeDeployer.primeRapp(any())).thenReturn(true);
        when(smeDeployer.primeRapp(any())).thenReturn(true);
        when(deploymentArtifactsService.configureDeploymentArtifacts(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, rappService.primeRapp(rapp).getStatusCode());
        assertEquals(RappState.PRIMED, rapp.getState());
    }

    @Test
    void testPrimeRappInvalidState() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMING).build();
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.primeRapp(rapp));
        assertEquals(HttpStatus.BAD_REQUEST, rappHandlerException.getStatusCode());
        assertEquals(String.format(stateTransitionNotPermitted, RappState.PRIMING, RappState.PRIMED),
                rappHandlerException.getMessage());
        assertEquals(RappState.PRIMING, rapp.getState());
    }

    @Test
    void testPrimeRappAcmFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        when(deploymentArtifactsService.configureDeploymentArtifacts(any())).thenReturn(true);
        when(acmDeployer.primeRapp(any())).thenReturn(false);
        when(dmeDeployer.primeRapp(any())).thenReturn(true);
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.primeRapp(rapp));
        assertEquals(HttpStatus.BAD_GATEWAY, rappHandlerException.getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testPrimeRappDmeFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        when(deploymentArtifactsService.configureDeploymentArtifacts(any())).thenReturn(true);
        when(acmDeployer.primeRapp(any())).thenReturn(true);
        when(dmeDeployer.primeRapp(any())).thenReturn(false);
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.primeRapp(rapp));
        assertEquals(HttpStatus.BAD_GATEWAY, rappHandlerException.getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testPrimeRappHelmUploadFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        doThrow(new RappValidationException("")).when(deploymentArtifactsService).configureDeploymentArtifacts(any());
        when(acmDeployer.primeRapp(any())).thenReturn(true);
        when(dmeDeployer.primeRapp(any())).thenReturn(false);
        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> rappService.primeRapp(rapp));
        assertEquals(HttpStatus.BAD_REQUEST, rappValidationException.getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testPrimeRappDeployArtifactFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        when(deploymentArtifactsService.configureDeploymentArtifacts(any())).thenReturn(false);
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.primeRapp(rapp));
        assertEquals(HttpStatus.BAD_GATEWAY, rappHandlerException.getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testDeprimeRapp() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        when(acmDeployer.deprimeRapp(any())).thenReturn(true);
        when(dmeDeployer.deprimeRapp(any())).thenReturn(true);
        when(smeDeployer.deprimeRapp(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, rappService.deprimeRapp(rapp).getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testDeprimeRappAcmFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        when(acmDeployer.deprimeRapp(any())).thenReturn(false);
        when(dmeDeployer.deprimeRapp(any())).thenReturn(true);
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deprimeRapp(rapp));
        assertEquals(HttpStatus.BAD_GATEWAY, rappHandlerException.getStatusCode());
        assertEquals(RappState.PRIMED, rapp.getState());
    }

    @Test
    void testDeprimeRappDmeFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        when(acmDeployer.deprimeRapp(any())).thenReturn(true);
        when(dmeDeployer.deprimeRapp(any())).thenReturn(false);
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deprimeRapp(rapp));
        assertEquals(HttpStatus.BAD_GATEWAY, rappHandlerException.getStatusCode());
        assertEquals(RappState.PRIMED, rapp.getState());
    }

    @Test
    void testDeprimeRappInvalidState() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deprimeRapp(rapp));
        assertEquals(HttpStatus.BAD_REQUEST, rappHandlerException.getStatusCode());
        assertEquals(String.format(stateTransitionNotPermitted, RappState.COMMISSIONED, RappState.COMMISSIONED),
                rappHandlerException.getMessage());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testDeprimeRappActiveInstances() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED)
                            .rappInstances(Map.of(UUID.randomUUID(), new RappInstance())).build();
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deprimeRapp(rapp));
        assertEquals(HttpStatus.BAD_REQUEST, rappHandlerException.getStatusCode());
        assertEquals(RappState.PRIMED, rapp.getState());
    }

    @Test
    void testDeployRappInstance() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        when(acmDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        assertEquals(HttpStatus.ACCEPTED, rappService.deployRappInstance(rapp, rappInstance).getStatusCode());
    }

    @Test
    void testDeployRappInstanceFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        when(acmDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.deployRappInstance(any(), any())).thenReturn(false);
        when(dmeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deployRappInstance(rapp, rappInstance));
        assertEquals(HttpStatus.BAD_GATEWAY, rappHandlerException.getStatusCode());
    }

    @Test
    void testDeployRappInstanceFailureWithState() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setState(RappInstanceState.DEPLOYED);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deployRappInstance(rapp, rappInstance));
        assertEquals(HttpStatus.BAD_REQUEST, rappHandlerException.getStatusCode());
        assertEquals(String.format("Unable to deploy rApp instance %s as it is not in UNDEPLOYED state",
                rappInstance.getRappInstanceId()), rappHandlerException.getMessage());
        assertEquals(RappState.PRIMED, rapp.getState());

    }

    @Test
    void testUndeployRappInstance() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setState(RappInstanceState.DEPLOYED);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        when(acmDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        assertEquals(HttpStatus.ACCEPTED, rappService.undeployRappInstance(rapp, rappInstance).getStatusCode());
    }

    @Test
    void testUndeployRappInstanceFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setState(RappInstanceState.DEPLOYED);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        when(acmDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.undeployRappInstance(any(), any())).thenReturn(false);
        when(dmeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.undeployRappInstance(rapp, rappInstance));
        assertEquals(HttpStatus.BAD_GATEWAY, rappHandlerException.getStatusCode());
    }

    @Test
    void testUndeployRappInstanceInvalidStateFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setState(RappInstanceState.DEPLOYING);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        when(acmDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.undeployRappInstance(any(), any())).thenReturn(false);
        when(dmeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.undeployRappInstance(rapp, rappInstance));
        assertEquals(HttpStatus.BAD_REQUEST, rappHandlerException.getStatusCode());
    }

    @Test
    void testDeleteRappInstance() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setState(RappInstanceState.UNDEPLOYED);
        HashMap<UUID, RappInstance> rAppInstanceMap = new HashMap<>();
        rAppInstanceMap.put(rappInstance.getRappInstanceId(), rappInstance);
        rapp.setRappInstances(rAppInstanceMap);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        assertEquals(HttpStatus.NO_CONTENT,
                rappService.deleteRappInstance(rapp, rappInstance.getRappInstanceId()).getStatusCode());
    }

    @Test
    void testDeleteRappInstanceFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setState(RappInstanceState.DEPLOYED);
        UUID rappInstanceId = rappInstance.getRappInstanceId();
        HashMap<UUID, RappInstance> rAppInstanceMap = new HashMap<>();
        rAppInstanceMap.put(rappInstanceId, rappInstance);
        rapp.setRappInstances(rAppInstanceMap);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deleteRappInstance(rapp, rappInstanceId));
        assertEquals(HttpStatus.BAD_REQUEST, rappHandlerException.getStatusCode());
        assertEquals(String.format("Unable to delete rApp instance %s as it is not in UNDEPLOYED state",
                rappInstance.getRappInstanceId()), rappHandlerException.getMessage());
        assertEquals(RappState.PRIMED, rapp.getState());
    }

    @Test
    void testDeleteRappSuccess() {
        Rapp rApp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        assertEquals(HttpStatus.OK, rappService.deleteRapp(rApp).getStatusCode());
    }

    @Test
    void testDeleteRappFailureWithState() {
        String rAppName = "rAppInPrimed";
        Rapp rApp = Rapp.builder().rappId(UUID.randomUUID()).name(rAppName).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deleteRapp(rApp));
        assertEquals(HttpStatus.BAD_REQUEST, rappHandlerException.getStatusCode());
        assertEquals(String.format("Unable to delete %s as the rApp is not in COMMISSIONED state.", rAppName),
                rappHandlerException.getMessage());
        assertEquals(RappState.PRIMED, rApp.getState());
    }

    @Test
    void testDeleteRappFailureWithInstances() {
        String rAppName = "rAppWithInstances";
        Rapp rApp = Rapp.builder().rappId(UUID.randomUUID()).name(rAppName).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setState(RappInstanceState.DEPLOYED);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        rApp.setRappInstances(Map.of(rappInstance.getRappInstanceId(), rappInstance));
        RappHandlerException rappHandlerException =
                assertThrows(RappHandlerException.class, () -> rappService.deleteRapp(rApp));
        assertEquals(HttpStatus.BAD_REQUEST, rappHandlerException.getStatusCode());
        assertEquals(String.format("Unable to delete %s as there are active rApp instances.", rAppName),
                rappHandlerException.getMessage());
        assertEquals(RappState.PRIMED, rApp.getState());
    }
}
