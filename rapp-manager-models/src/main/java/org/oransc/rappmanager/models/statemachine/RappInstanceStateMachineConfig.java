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

import org.oransc.rappmanager.models.rapp.RappEvent;
import org.oransc.rappmanager.models.rappinstance.RappInstanceState;
import java.util.EnumSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

@Configuration
@EnableStateMachineFactory
public class RappInstanceStateMachineConfig extends EnumStateMachineConfigurerAdapter<RappInstanceState, RappEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<RappInstanceState, RappEvent> states) throws Exception {
        states.withStates().initial(RappInstanceState.UNDEPLOYED).states(EnumSet.allOf(RappInstanceState.class));
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<RappInstanceState, RappEvent> config) throws Exception {
        config.withConfiguration();
    }

    // @formatter:off
    @Override
    public void configure(StateMachineTransitionConfigurer<RappInstanceState, RappEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(RappInstanceState.UNDEPLOYED).target(RappInstanceState.DEPLOYING).event(RappEvent.DEPLOYING)
                    .and()
                .withExternal()
                    .source(RappInstanceState.DEPLOYING).target(RappInstanceState.UNDEPLOYED).event(RappEvent.ACMDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappInstanceState.DEPLOYING).target(RappInstanceState.UNDEPLOYED).event(RappEvent.SMEDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappInstanceState.DEPLOYING).target(RappInstanceState.UNDEPLOYED).event(RappEvent.DMEDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappInstanceState.UNDEPLOYING).target(RappInstanceState.DEPLOYED).event(RappEvent.ACMUNDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappInstanceState.UNDEPLOYING).target(RappInstanceState.DEPLOYED).event(RappEvent.SMEUNDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappInstanceState.UNDEPLOYING).target(RappInstanceState.DEPLOYED).event(RappEvent.DMEUNDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappInstanceState.DEPLOYED).target(RappInstanceState.UNDEPLOYING).event(RappEvent.UNDEPLOYING)
                    .and()
                .withExternal()
                    .source(RappInstanceState.DEPLOYING).target(RappInstanceState.DEPLOYED).event(RappEvent.ACMDEPLOYED)
                    .guard(deployedGuard())
                    .and()
                .withExternal()
                    .source(RappInstanceState.DEPLOYING).target(RappInstanceState.DEPLOYED).event(RappEvent.SMEDEPLOYED)
                    .guard(deployedGuard())
                    .and()
                .withExternal()
                    .source(RappInstanceState.DEPLOYING).target(RappInstanceState.DEPLOYED).event(RappEvent.DMEDEPLOYED)
                    .guard(deployedGuard())
                    .and()
                .withExternal()
                    .source(RappInstanceState.UNDEPLOYING).target(RappInstanceState.UNDEPLOYED).event(RappEvent.ACMUNDEPLOYED)
                    .guard(undeployedGuard())
                    .and()
                .withExternal()
                    .source(RappInstanceState.UNDEPLOYING).target(RappInstanceState.UNDEPLOYED).event(RappEvent.SMEUNDEPLOYED)
                    .guard(undeployedGuard())
                    .and()
                .withExternal()
                    .source(RappInstanceState.UNDEPLOYING).target(RappInstanceState.UNDEPLOYED).event(RappEvent.DMEUNDEPLOYED)
                    .guard(undeployedGuard());

    }
    // @formatter:on

    @Bean
    public Guard<RappInstanceState, RappEvent> deployedGuard() {
        return stateContext -> {
            stateContext.getExtendedState().getVariables().put(stateContext.getEvent(), true);
            return stateContext.getExtendedState().getVariables().get(RappEvent.ACMDEPLOYED) != null && smeGuard(
                    stateContext, RappEvent.SMEDEPLOYED) && dmeGuard(stateContext, RappEvent.DMEDEPLOYED);
        };
    }

    boolean smeGuard(StateContext<RappInstanceState, RappEvent> stateContext, RappEvent rappEvent) {
        Boolean smeEnabled = (Boolean) stateContext.getExtendedState().getVariables().get("sme");
        if (smeEnabled != null && smeEnabled.equals(Boolean.TRUE)) {
            return stateContext.getExtendedState().getVariables().get(rappEvent) != null;
        }
        return true;
    }

    boolean dmeGuard(StateContext<RappInstanceState, RappEvent> stateContext, RappEvent rappEvent) {
        Boolean dmeEnabled = (Boolean) stateContext.getExtendedState().getVariables().get("dme");
        if (dmeEnabled != null && dmeEnabled.equals(Boolean.TRUE)) {
            return stateContext.getExtendedState().getVariables().get(rappEvent) != null;
        }
        return true;
    }

    @Bean
    public Guard<RappInstanceState, RappEvent> undeployedGuard() {
        return stateContext -> {
            stateContext.getExtendedState().getVariables().put(stateContext.getEvent(), true);
            return stateContext.getExtendedState().getVariables().get(RappEvent.ACMUNDEPLOYED) != null && smeGuard(
                    stateContext, RappEvent.SMEUNDEPLOYED) && dmeGuard(stateContext, RappEvent.DMEUNDEPLOYED);
        };
    }
}
