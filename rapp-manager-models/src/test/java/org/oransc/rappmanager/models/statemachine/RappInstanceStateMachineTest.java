/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023-2024 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.models.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.rappmanager.models.rapp.RappEvent;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstanceState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RappInstanceStateMachine.class, RappInstanceStateMachineConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RappInstanceStateMachineTest {

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
