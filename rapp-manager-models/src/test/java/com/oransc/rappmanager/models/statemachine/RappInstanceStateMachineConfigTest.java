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

package com.oransc.rappmanager.models.statemachine;

import com.oransc.rappmanager.models.rapp.RappEvent;
import com.oransc.rappmanager.models.rappinstance.RappInstanceState;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RappInstanceStateMachineConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RappInstanceStateMachineConfigTest {

    @Autowired
    StateMachineFactory<RappInstanceState, RappEvent> stateMachineFactory;

    StateMachine<RappInstanceState, RappEvent> stateMachine;

    @BeforeEach
    void getStateMachine() {
        stateMachine = stateMachineFactory.getStateMachine(UUID.randomUUID());
        stateMachine.startReactively().subscribe();
    }

    @AfterEach
    void stopStateMachine() {
        stateMachine.stopReactively().subscribe();
    }

    @Test
    void testOnboardedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().build();
        plan.test();
    }

    @Test
    void testDeployingState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().build();
        plan.test();
    }

    @ParameterizedTest
    @EnumSource(value = RappEvent.class, names = {"ACMDEPLOYED", "SMEDEPLOYED"})
    void testIndividualDeployedState(RappEvent rappEvent) throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(rappEvent).expectState(RappInstanceState.DEPLOYING).and().build();
        plan.test();
    }

    @Test
    void testDeployedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().build();
        plan.test();
    }

    @Test
    void testDeployFailedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYFAILED).expectState(RappInstanceState.UNDEPLOYED)
                        .expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeployingState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().build();
        plan.test();
    }

    @ParameterizedTest
    @EnumSource(value = RappEvent.class, names = {"ACMUNDEPLOYED", "SMEUNDEPLOYED"})
    void testIndividualUndeployedState(RappEvent rappEvent) throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(rappEvent)
                        .expectState(RappInstanceState.UNDEPLOYING).and().build();
        plan.test();
    }

    @Test
    void testUndeployedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.SMEUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeployFailedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.SMEUNDEPLOYFAILED)
                        .expectState(RappInstanceState.DEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }
}
