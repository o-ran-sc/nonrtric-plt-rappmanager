package com.oransc.rappmanager.models.statemachine;

import com.oransc.rappmanager.models.RappEvent;
import com.oransc.rappmanager.models.RappState;
import java.util.EnumSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

@Configuration
@EnableStateMachineFactory
public class RappStateMachineConfig extends EnumStateMachineConfigurerAdapter<RappState, RappEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<RappState, RappEvent> states) throws Exception {
        states.withStates().initial(RappState.ONBOARDED).states(EnumSet.allOf(RappState.class));
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<RappState, RappEvent> config) throws Exception {
        config.withConfiguration();
    }

    // @formatter:off
    @Override
    public void configure(StateMachineTransitionConfigurer<RappState, RappEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(RappState.ONBOARDED).target(RappState.DEPLOYING).event(RappEvent.DEPLOYING)
                    .and()
                .withExternal()
                    .source(RappState.DEPLOYING).target(RappState.FAILED).event(RappEvent.ACMDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappState.DEPLOYING).target(RappState.FAILED).event(RappEvent.SMEDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappState.UNDEPLOYING).target(RappState.FAILED).event(RappEvent.ACMUNDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappState.UNDEPLOYING).target(RappState.FAILED).event(RappEvent.SMEUNDEPLOYFAILED)
                    .and()
                .withExternal()
                    .source(RappState.DEPLOYED).target(RappState.UNDEPLOYING).event(RappEvent.UNDEPLOYING)
                    .and()
                .withExternal()
                    .source(RappState.DEPLOYING).target(RappState.DEPLOYED).event(RappEvent.ACMDEPLOYED)
                    .guard(deployedGuard())
                    .and()
                .withExternal()
                    .source(RappState.DEPLOYING).target(RappState.DEPLOYED).event(RappEvent.SMEDEPLOYED)
                    .guard(deployedGuard())
                    .and()
                .withExternal()
                    .source(RappState.UNDEPLOYING).target(RappState.UNDEPLOYED).event(RappEvent.ACMUNDEPLOYED)
                    .guard(undeployedGuard())
                    .and()
                .withExternal()
                    .source(RappState.UNDEPLOYING).target(RappState.UNDEPLOYED).event(RappEvent.SMEUNDEPLOYED)
                    .guard(undeployedGuard());

    }
    // @formatter:on

    @Bean
    public Guard<RappState, RappEvent> deployedGuard() {
        return stateContext -> {
            stateContext.getExtendedState().getVariables().put(stateContext.getEvent(), true);
            return stateContext.getExtendedState().getVariables().get(RappEvent.ACMDEPLOYED) != null
                           && stateContext.getExtendedState().getVariables().get(RappEvent.SMEDEPLOYED) != null;
        };
    }

    @Bean
    public Guard<RappState, RappEvent> undeployedGuard() {
        return stateContext -> {
            stateContext.getExtendedState().getVariables().put(stateContext.getEvent(), true);
            return stateContext.getExtendedState().getVariables().get(RappEvent.ACMUNDEPLOYED) != null
                           && stateContext.getExtendedState().getVariables().get(RappEvent.SMEUNDEPLOYED) != null;
        };
    }
}
