/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

package com.oransc.rappmanager.service;

import com.oransc.rappmanager.acm.service.AcmDeployer;
import com.oransc.rappmanager.models.RappDeployer;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.exception.RappHandlerException;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappEvent;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstanceState;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RappService {

    private final AcmDeployer acmDeployer;
    private final List<RappDeployer> rappDeployers;
    private final RappInstanceStateMachine rappInstanceStateMachine;
    private final RappCacheService rappCacheService;
    private static final String STATE_TRANSITION_NOT_PERMITTED = "State transition from %s to %s is not permitted.";

    public ResponseEntity<String> primeRapp(Rapp rapp) {
        if (rapp.getState().equals(RappState.COMMISSIONED)) {
            rapp.setState(RappState.PRIMING);
            rapp.setReason(null);
            if (rappDeployers.parallelStream().allMatch(rappDeployer -> rappDeployer.primeRapp(rapp))) {
                rapp.setState(RappState.PRIMED);
                return ResponseEntity.ok().build();
            }
            rapp.setState(RappState.COMMISSIONED);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                String.format(STATE_TRANSITION_NOT_PERMITTED, rapp.getState().name(), RappState.PRIMED.name()));

    }

    public ResponseEntity<String> deprimeRapp(Rapp rapp) {
        if (rapp.getState().equals(RappState.PRIMED) && rapp.getRappInstances().isEmpty()) {
            rapp.setState(RappState.DEPRIMING);
            rapp.setReason(null);
            if (rappDeployers.parallelStream().allMatch(rappDeployer -> rappDeployer.deprimeRapp(rapp))) {
                rapp.setState(RappState.COMMISSIONED);
                return ResponseEntity.ok().build();
            }
            rapp.setState(RappState.PRIMED);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        if (!rapp.getRappInstances().isEmpty()) {
            throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                    "Unable to deprime as there are active rapp instances.");
        } else {
            throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                    String.format(STATE_TRANSITION_NOT_PERMITTED, RappState.COMMISSIONED.name(),
                            rapp.getState().name()));
        }
    }

    public ResponseEntity<String> deleteRapp(Rapp rApp) {
        if (rApp.getRappInstances().isEmpty() && rApp.getState().equals(RappState.COMMISSIONED)) {
            rappCacheService.deleteRapp(rApp);
            return ResponseEntity.ok().build();
        }
        if (!rApp.getRappInstances().isEmpty()) {
            throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                    String.format("Unable to delete %s as there are active rApp instances.", rApp.getName()));
        } else {
            throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                    String.format("Unable to delete %s as the rApp is not in COMMISSIONED state.", rApp.getName()));
        }

    }

    public ResponseEntity<String> deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        if (rappInstance.getState().equals(RappInstanceState.UNDEPLOYED)) {
            rappInstance.setReason(null);
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.DEPLOYING);
            if (rappDeployers.parallelStream()
                        .allMatch(rappDeployer -> rappDeployer.deployRappInstance(rapp, rappInstance))) {
                return ResponseEntity.accepted().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                String.format("Unable to deploy rApp instance %s as it is not in UNDEPLOYED state",
                        rappInstance.getRappInstanceId()));

    }

    public ResponseEntity<String> undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        if (rappInstance.getState().equals(RappInstanceState.DEPLOYED)) {
            rappInstance.setReason(null);
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.UNDEPLOYING);
            if (rappDeployers.parallelStream()
                        .allMatch(rappDeployer -> rappDeployer.undeployRappInstance(rapp, rappInstance))) {
                return ResponseEntity.accepted().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                String.format("Unable to undeploy rApp instance %s as it is not in DEPLOYED state",
                        rappInstance.getRappInstanceId()));
    }

    public ResponseEntity<String> deleteRappInstance(Rapp rApp, UUID rappInstanceId) {
        if (rApp.getRappInstances().get(rappInstanceId).getState().equals(RappInstanceState.UNDEPLOYED)) {
            rappInstanceStateMachine.deleteRappInstance(rApp.getRappInstances().get(rappInstanceId));
            rApp.getRappInstances().remove(rappInstanceId);
            return ResponseEntity.noContent().build();
        }
        throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                String.format("Unable to delete rApp instance %s as it is not in UNDEPLOYED state", rappInstanceId));
    }

    public void updateRappInstanceState(Rapp rapp, RappInstance rappInstance) {
        acmDeployer.syncRappInstanceStatus(rapp.getCompositionId(), rappInstance);
    }
}
