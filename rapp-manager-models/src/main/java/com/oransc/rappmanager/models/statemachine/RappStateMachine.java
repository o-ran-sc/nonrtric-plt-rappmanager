package com.oransc.rappmanager.models.statemachine;

import com.oransc.rappmanager.models.Rapp;
import com.oransc.rappmanager.models.RappEvent;
import com.oransc.rappmanager.models.RappState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RappStateMachine {

    Logger logger = LoggerFactory.getLogger(RappStateMachine.class);

    private final StateMachineFactory<RappState, RappEvent> stateMachineFactory;
    private Map<UUID, StateMachine<RappState, RappEvent>> stateMachineMap = new HashMap<>();

    public void onboardRapp(UUID rappId) {
        StateMachine<RappState, RappEvent> stateMachine = stateMachineFactory.getStateMachine(rappId);
        stateMachineMap.put(rappId, stateMachine);
        stateMachine.start();
    }

    public void sendRappEvent(Rapp rapp, RappEvent rappEvent) {
        logger.info("Sending rapp event {} for {}", rappEvent.name(), rapp.getRappId());
        logger.info("State machine map is {}", stateMachineMap);
        stateMachineMap.get(rapp.getRappId()).sendEvent(rappEvent);
    }

    public RappState getRappState(UUID rappId) {
        return stateMachineMap.get(rappId).getState().getId();
    }

    public void deleteRapp(Rapp rapp) {
        stateMachineMap.get(rapp.getRappId()).stop();
    }
}
