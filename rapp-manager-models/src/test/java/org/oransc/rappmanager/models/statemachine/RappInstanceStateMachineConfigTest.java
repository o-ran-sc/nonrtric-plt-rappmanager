/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.oransc.rappmanager.models.rapp.RappEvent;
import org.oransc.rappmanager.models.rappinstance.RappInstanceState;
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
        stateMachine.getExtendedState().getVariables().put("sme", true);
        stateMachine.getExtendedState().getVariables().put("dme", true);
        stateMachine.startReactively().subscribe();
    }

    @AfterEach
    void stopStateMachine() {
        stateMachine.stopReactively().subscribe();
    }

    @Test
    void testOnboardedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().build();
        plan.test();
    }

    @Test
    void testDeployingState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().build();
        plan.test();
    }

    @ParameterizedTest
    @EnumSource(value = RappEvent.class, names = {"ACMDEPLOYED", "SMEDEPLOYED", "DMEDEPLOYED"})
    void testIndividualDeployedState(RappEvent rappEvent) throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(rappEvent).expectState(RappInstanceState.DEPLOYING).and().build();
        plan.test();
    }

    @Test
    void testDeployedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().build();
        plan.test();
    }

    @Test
    void testDeployedStateAcmOnly() throws Exception {
        stateMachine.getExtendedState().getVariables().put("sme", false);
        stateMachine.getExtendedState().getVariables().put("dme", false);
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().build();
        plan.test();
    }

    @Test
    void testDeployedStateAcmOnlyWithNoKeyReference() throws Exception {
        stateMachine.getExtendedState().getVariables().remove("sme");
        stateMachine.getExtendedState().getVariables().remove("dme");
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().build();
        plan.test();
    }

    @Test
    void testDeployedStateAcmAndSmeOnly() throws Exception {
        stateMachine.getExtendedState().getVariables().put("dme", false);
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().build();
        plan.test();
    }

    @Test
    void testDeployedStateAcmAndDmeOnly() throws Exception {
        stateMachine.getExtendedState().getVariables().put("sme", false);
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().build();
        plan.test();
    }

    @Test
    void testAcmDeployFailedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYFAILED).expectState(RappInstanceState.UNDEPLOYED)
                        .expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testSmeDeployFailedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYFAILED).expectState(RappInstanceState.UNDEPLOYED)
                        .expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testDmeDeployFailedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYFAILED).expectState(RappInstanceState.UNDEPLOYED)
                        .expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeployingState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().build();
        plan.test();
    }

    @ParameterizedTest
    @EnumSource(value = RappEvent.class, names = {"ACMUNDEPLOYED", "SMEUNDEPLOYED", "DMEUNDEPLOYED"})
    void testIndividualUndeployedState(RappEvent rappEvent) throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(rappEvent)
                        .expectState(RappInstanceState.UNDEPLOYING).and().build();
        plan.test();
    }

    @Test
    void testUndeployedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.SMEUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.DMEUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }


    @Test
    void testUndeployedStateAcmOnly() throws Exception {
        stateMachine.getExtendedState().getVariables().put("sme", false);
        stateMachine.getExtendedState().getVariables().put("dme", false);
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeployedStateAcmOnlyWithNoKeyReference() throws Exception {
        stateMachine.getExtendedState().getVariables().remove("sme");
        stateMachine.getExtendedState().getVariables().remove("dme");
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeployedStateAcmAndSmeOnly() throws Exception {
        stateMachine.getExtendedState().getVariables().put("dme", false);
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
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
    void testUndeployedStateAcmAndDmeOnly() throws Exception {
        stateMachine.getExtendedState().getVariables().put("sme", false);
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.DMEUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeployAcmFailedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.SMEUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.DMEUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.ACMUNDEPLOYFAILED)
                        .expectState(RappInstanceState.DEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeploySmeFailedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.DMEUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.SMEUNDEPLOYFAILED)
                        .expectState(RappInstanceState.DEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }

    @Test
    void testUndeployDmeFailedState() throws Exception {
        StateMachineTestPlan<RappInstanceState, RappEvent> plan =
                StateMachineTestPlanBuilder.<RappInstanceState, RappEvent>builder().stateMachine(stateMachine).step()
                        .expectState(RappInstanceState.UNDEPLOYED).and().step().sendEvent(RappEvent.DEPLOYING)
                        .expectState(RappInstanceState.DEPLOYING).expectStateChanged(1).and().step()
                        .sendEvent(RappEvent.ACMDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.SMEDEPLOYED).expectState(RappInstanceState.DEPLOYING).and().step()
                        .sendEvent(RappEvent.DMEDEPLOYED).expectState(RappInstanceState.DEPLOYED).expectStateChanged(1)
                        .and().step().sendEvent(RappEvent.UNDEPLOYING).expectState(RappInstanceState.UNDEPLOYING)
                        .expectStateChanged(1).and().step().sendEvent(RappEvent.ACMUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.SMEUNDEPLOYED)
                        .expectState(RappInstanceState.UNDEPLOYING).and().step().sendEvent(RappEvent.DMEUNDEPLOYFAILED)
                        .expectState(RappInstanceState.DEPLOYED).expectStateChanged(1).and().build();
        plan.test();
    }
}
