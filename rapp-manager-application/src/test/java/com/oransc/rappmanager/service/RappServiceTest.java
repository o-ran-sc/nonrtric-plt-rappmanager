package com.oransc.rappmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.oransc.rappmanager.acm.service.AcmDeployer;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstanceState;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import com.oransc.rappmanager.sme.service.SmeDeployer;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RappServiceTest {

    @Autowired
    RappService rappService;

    @MockBean
    AcmDeployer acmDeployer;

    @MockBean
    SmeDeployer smeDeployer;

    @Autowired
    RappInstanceStateMachine rappInstanceStateMachine;

    String validCsarFileLocation = "src/test/resources/";

    private final String validRappFile = "valid-rapp-package.csar";

    private final String invalidRappFile = "invalid-rapp-package.csar";


    @Test
    void testPrimeRapp() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        when(acmDeployer.primeRapp(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, rappService.primeRapp(rapp).getStatusCode());
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
        assertEquals(HttpStatus.OK, rappService.primeRapp(rapp).getStatusCode());
    }

    @Test
    void testDeprimeRapp() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        when(acmDeployer.deprimeRapp(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, rappService.deprimeRapp(rapp).getStatusCode());
    }

    @Test
    void testDeprimeRappFailure() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        when(acmDeployer.deprimeRapp(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, rappService.deprimeRapp(rapp).getStatusCode());
    }

    @Test
    void testDeprimeRappInvalidState() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        assertEquals(HttpStatus.BAD_REQUEST, rappService.deprimeRapp(rapp).getStatusCode());
    }

    @Test
    void testDeprimeRappActiveInstances() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .rappInstances(Map.of(UUID.randomUUID(), new RappInstance())).build();
        assertEquals(HttpStatus.BAD_REQUEST, rappService.deprimeRapp(rapp).getStatusCode());
    }

    @Test
    void testDeployRappInstance() {
        Rapp rapp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        RappInstance rappInstance = new RappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        when(acmDeployer.deployRappInstance(any(), any())).thenReturn(true);
        when(smeDeployer.deployRappInstance(any(), any())).thenReturn(true);
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
        assertEquals(HttpStatus.BAD_GATEWAY, rappService.deployRappInstance(rapp, rappInstance).getStatusCode());
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
        assertEquals(HttpStatus.BAD_REQUEST, rappService.undeployRappInstance(rapp, rappInstance).getStatusCode());
    }
}
