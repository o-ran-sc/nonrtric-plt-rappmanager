package com.oransc.rappmanager.models.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.oransc.rappmanager.models.rapp.RappEvent;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstanceState;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RappInstanceStateMachine.class, RappInstanceStateMachineConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RappInstanceStateMachineTest {

    @Autowired
    RappInstanceStateMachine rappInstanceStateMachine;

    @Test
    void testOnboardRappInstance() {
        UUID rappInstanceId = UUID.randomUUID();
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        assertNotNull(rappInstanceStateMachine.stateMachineMap.get(rappInstanceId));
    }

    @Test
    void testSendRappInstanceEvent() {
        UUID rappInstanceId = UUID.randomUUID();
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        assertEquals(RappInstanceState.UNDEPLOYED, rappInstanceStateMachine.getRappInstanceState(rappInstanceId));
        RappInstance rappInstance = new RappInstance();
        rappInstance.setRappInstanceId(rappInstanceId);
        rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.DEPLOYING);
        assertEquals(RappInstanceState.DEPLOYING, rappInstanceStateMachine.getRappInstanceState(rappInstanceId));
    }

    @Test
    void testGetRappInstanceState() {
        UUID rappInstanceId = UUID.randomUUID();
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        assertEquals(RappInstanceState.UNDEPLOYED, rappInstanceStateMachine.getRappInstanceState(rappInstanceId));
    }

    @Test
    void testDeleteRappInstance() {
        UUID rappInstanceId = UUID.randomUUID();
        rappInstanceStateMachine.onboardRappInstance(rappInstanceId);
        assertNotNull(rappInstanceStateMachine.stateMachineMap.get(rappInstanceId));
        RappInstance rappInstance = new RappInstance();
        rappInstance.setRappInstanceId(rappInstanceId);
        rappInstanceStateMachine.deleteRappInstance(rappInstance);
        assertNull(rappInstanceStateMachine.stateMachineMap.get(rappInstanceId));
    }
}
