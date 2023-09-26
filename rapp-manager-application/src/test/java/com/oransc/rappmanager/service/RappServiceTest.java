package com.oransc.rappmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.oransc.rappmanager.acm.service.AcmDeployer;
import com.oransc.rappmanager.dme.service.DmeDeployer;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstanceState;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import com.oransc.rappmanager.sme.service.SmeDeployer;
import com.oransc.rappmanager.sme.service.SmeLifecycleManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RappServiceTest {

    @Autowired
    RappService rappService;

    @MockBean
    AcmDeployer acmDeployer;

    @MockBean
    SmeDeployer smeDeployer;

    @MockBean
    DmeDeployer dmeDeployer;

    @MockBean
    SmeLifecycleManager smeLifecycleManager;

    @Autowired
    RappInstanceStateMachine rappInstanceStateMachine;

    String validCsarFileLocation = "src/test/resources/";

    private final String validRappFile = "valid-rapp-package.csar";


    @Test
    void testPrimeRapp() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        when(acmDeployer.primeRapp(any())).thenReturn(true);
        when(dmeDeployer.primeRapp(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, rappService.primeRapp(rapp).getStatusCode());
        assertEquals(RappState.PRIMED, rapp.getState());
    }

    @Test
    void testPrimeRappInvalidState() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMING).build();
        assertEquals(HttpStatus.BAD_REQUEST, rappService.primeRapp(rapp).getStatusCode());
    }

    @Test
    void testPrimeRappAcmFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        when(acmDeployer.primeRapp(any())).thenReturn(false);
        when(dmeDeployer.primeRapp(any())).thenReturn(true);
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.primeRapp(rapp).getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testPrimeRappDmeFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        when(acmDeployer.primeRapp(any())).thenReturn(true);
        when(dmeDeployer.primeRapp(any())).thenReturn(false);
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.primeRapp(rapp).getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }


    @Test
    void testDeprimeRapp() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        when(acmDeployer.deprimeRapp(any())).thenReturn(true);
        when(dmeDeployer.deprimeRapp(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, rappService.deprimeRapp(rapp).getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testDeprimeRappAcmFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        when(acmDeployer.deprimeRapp(any())).thenReturn(false);
        when(dmeDeployer.deprimeRapp(any())).thenReturn(true);
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.deprimeRapp(rapp).getStatusCode());
        assertEquals(RappState.PRIMED, rapp.getState());
    }

    @Test
    void testDeprimeRappDmeFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        when(acmDeployer.deprimeRapp(any())).thenReturn(true);
        when(dmeDeployer.deprimeRapp(any())).thenReturn(false);
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.deprimeRapp(rapp).getStatusCode());
        assertEquals(RappState.PRIMED, rapp.getState());
    }

    @Test
    void testDeprimeRappInvalidState() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        assertEquals(HttpStatus.BAD_REQUEST, rappService.deprimeRapp(rapp).getStatusCode());
        assertEquals(RappState.COMMISSIONED, rapp.getState());
    }

    @Test
    void testDeprimeRappActiveInstances() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED)
                            .rappInstances(Map.of(UUID.randomUUID(), new RappInstance())).build();
        assertEquals(HttpStatus.BAD_REQUEST, rappService.deprimeRapp(rapp).getStatusCode());
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
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.deployRappInstance(rapp, rappInstance).getStatusCode());
    }

    @Test
    void testDeployRappInstanceDmeFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        when(acmDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.deployRappInstance(any(), any())).thenReturn(false);
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.deployRappInstance(rapp, rappInstance).getStatusCode());
    }

    @Test
    void testDeployRappInstanceFailureWithState() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        RappInstanceState rappInstanceState = RappInstanceState.DEPLOYED;
        rappInstance.setState(rappInstanceState);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        ResponseEntity<String> responseEntity = rappService.deployRappInstance(rapp, rappInstance);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("State transition from " + rappInstanceState + " to DEPLOYED is not permitted.",
                responseEntity.getBody());
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
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.undeployRappInstance(rapp, rappInstance).getStatusCode());
    }

    @Test
    void testUndeployRappInstanceDmeFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstance.setState(RappInstanceState.DEPLOYED);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        when(acmDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.undeployRappInstance(any(), any())).thenReturn(true);
        when(dmeDeployer.undeployRappInstance(any(), any())).thenReturn(false);
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.undeployRappInstance(rapp, rappInstance).getStatusCode());
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
        assertEquals(HttpStatus.BAD_REQUEST, rappService.undeployRappInstance(rapp, rappInstance).getStatusCode());
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
        ResponseEntity<String> responseEntity = rappService.deleteRapp(rApp);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Unable to delete '" + rAppName + "' as the rApp is not in COMMISSIONED state.",
                responseEntity.getBody());
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
        ResponseEntity<String> responseEntity = rappService.deleteRapp(rApp);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Unable to delete '" + rAppName + "' as there are active rApp instances.",
                responseEntity.getBody());
    }
}
