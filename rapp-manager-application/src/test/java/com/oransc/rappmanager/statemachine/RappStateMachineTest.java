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

package com.oransc.rappmanager.statemachine;

import com.oransc.rappmanager.models.RappEvent;
import com.oransc.rappmanager.models.RappState;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

@SpringBootTest
public class RappStateMachineTest {

    @Autowired
    StateMachineFactory<RappState, RappEvent> stateMachineFactory;

    StateMachine<RappState, RappEvent> stateMachine;

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
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().build();
        plan.test();
    }

    @Test
    void testDeployingState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappState.DEPLOYING).expectStateChanged(1).and().build();
        plan.test();
    }

    @ParameterizedTest
    @EnumSource(value = RappEvent.class, names = {"ACMDEPLOYED", "SMEDEPLOYED"})
    void testIndividualDeployedState(RappEvent rappEvent) throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappState.DEPLOYING).expectStateChanged(1).and().step().sendEvent(rappEvent)
                        .expectState(RappState.DEPLOYING).and().build();
        plan.test();
    }

    @Test
    void testDeployedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappState.DEPLOYED).expectStateChanged(1).and()
                        .build();
        plan.test();
    }

    @Test
    void testDeployFailedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYFAILED).expectState(RappState.FAILED).expectStateChanged(1).and()
                        .build();
        plan.test();
    }

    @Test
    void testUndeployingState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappState.DEPLOYED).expectStateChanged(1).and()
                        .step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappState.UNDEPLOYING)
                        .expectStateChanged(1).and().build();
        plan.test();
    }

    @ParameterizedTest
    @EnumSource(value = RappEvent.class, names = {"ACMUNDEPLOYED", "SMEUNDEPLOYED"})
    void testIndividualUndeployedState(RappEvent rappEvent) throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappState.DEPLOYED).expectStateChanged(1).and()
                        .step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(rappEvent).expectState(RappState.UNDEPLOYING)
                        .and().build();
        plan.test();
    }

    @Test
    void testUndeployedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappState.DEPLOYED).expectStateChanged(1).and()
                        .step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappState.UNDEPLOYING).and().step().sendEvent(RappEvent.SMEUNDEPLOYED)
                        .expectState(RappState.UNDEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeployFailedState() throws Exception {
        StateMachineTestPlan plan =
                StateMachineTestPlanBuilder.<RappState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappState.ONBOARDED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappState.DEPLOYED).expectStateChanged(1).and()
                        .step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappState.UNDEPLOYING).and().step().sendEvent(RappEvent.SMEUNDEPLOYFAILED)
                        .expectState(RappState.FAILED).expectStateChanged(1).and().build();
        plan.test();
    }


}
