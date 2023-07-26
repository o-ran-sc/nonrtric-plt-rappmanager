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
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstanceState;
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
public class RappInstanceStateMachine {

    Logger logger = LoggerFactory.getLogger(RappInstanceStateMachine.class);

    private final StateMachineFactory<RappInstanceState, RappEvent> stateMachineFactory;
    Map<UUID, StateMachine<RappInstanceState, RappEvent>> stateMachineMap = new HashMap<>();

    public void onboardRappInstance(UUID rappInstanceId) {
        StateMachine<RappInstanceState, RappEvent> stateMachine = stateMachineFactory.getStateMachine(rappInstanceId);
        stateMachineMap.put(rappInstanceId, stateMachine);
        stateMachine.start();
    }

    public void sendRappInstanceEvent(RappInstance rappInstance, RappEvent rappEvent) {
        logger.info("Sending rapp instance event {} for {}", rappEvent.name(), rappInstance.getRappInstanceId());
        logger.debug("State machine map is {}", stateMachineMap);
        stateMachineMap.get(rappInstance.getRappInstanceId()).sendEvent(rappEvent);
    }

    public RappInstanceState getRappInstanceState(UUID rappInstanceId) {
        return stateMachineMap.get(rappInstanceId).getState().getId();
    }

    public void deleteRappInstance(RappInstance rappInstance) {
        stateMachineMap.get(rappInstance.getRappInstanceId()).stop();
        stateMachineMap.remove(rappInstance.getRappInstanceId());
    }
}
