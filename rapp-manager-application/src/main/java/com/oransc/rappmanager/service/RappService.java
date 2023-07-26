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

package com.oransc.rappmanager.service;

import com.oransc.rappmanager.acm.service.AcmDeployer;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappEvent;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstanceState;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import com.oransc.rappmanager.sme.service.SmeDeployer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RappService {

    private final AcmDeployer acmDeployer;
    private final SmeDeployer smeDeployer;
    private final RappInstanceStateMachine rappInstanceStateMachine;

    public ResponseEntity<String> primeRapp(Rapp rapp) {
        if (rapp.getState().equals(RappState.COMMISSIONED)) {
            rapp.setState(RappState.PRIMING);
            if (!acmDeployer.primeRapp(rapp)) {
                rapp.setState(RappState.COMMISSIONED);
            }
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest()
                           .body("State transition from " + RappState.PRIMED.name() + " to " + rapp.getState().name()
                                         + " is not permitted.");
        }
    }

    public ResponseEntity<String> deprimeRapp(Rapp rapp) {
        if (rapp.getState().equals(RappState.PRIMED) && rapp.getRappInstances().isEmpty()) {
            rapp.setState(RappState.DEPRIMING);
            if (!acmDeployer.deprimeRapp(rapp)) {
                rapp.setState(RappState.PRIMED);
            }
            return ResponseEntity.ok().build();
        } else {
            if (!rapp.getRappInstances().isEmpty()) {
                return ResponseEntity.badRequest().body("Unable to deprime as there are active rapp instances,");
            } else {
                return ResponseEntity.badRequest()
                               .body("State transition from " + RappState.COMMISSIONED.name() + " to " + rapp.getState()
                                                                                                                 .name()
                                             + " is not permitted.");
            }
        }
    }

    public ResponseEntity<String> deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        if (rappInstance.getState().equals(RappInstanceState.UNDEPLOYED)) {
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.DEPLOYING);
            if (acmDeployer.deployRappInstance(rapp, rappInstance) && smeDeployer.deployRappInstance(rapp,
                    rappInstance)) {
                return ResponseEntity.accepted().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } else {
            return ResponseEntity.badRequest().body("State transition from " + rappInstance.getState().name() + " to "
                                                            + RappInstanceState.DEPLOYED.name() + " is not permitted.");
        }
    }

    public ResponseEntity<String> undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        if (rappInstance.getState().equals(RappInstanceState.DEPLOYED)) {
            rappInstanceStateMachine.sendRappInstanceEvent(rappInstance, RappEvent.UNDEPLOYING);
            if (acmDeployer.undeployRappInstance(rapp, rappInstance) && smeDeployer.undeployRappInstance(rapp,
                    rappInstance)) {
                return ResponseEntity.accepted().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } else {
            return ResponseEntity.badRequest().body("State transition from " + rappInstance.getState().name() + " to "
                                                            + RappInstanceState.UNDEPLOYED.name()
                                                            + " is not permitted.");
        }
    }

    public void updateRappInstanceState(Rapp rapp, RappInstance rappInstance) {
        acmDeployer.syncRappInstanceStatus(rapp.getCompositionId(), rappInstance);
    }
}
