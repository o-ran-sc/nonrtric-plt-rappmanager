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

package com.oransc.rappmanager.rest;

import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.exception.RappHandlerException;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rappinstance.DeployOrder;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstanceDeployOrder;
import com.oransc.rappmanager.models.rappinstance.RappInstanceState;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import com.oransc.rappmanager.service.RappService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "rapps/{rapp_id}/instance")
@RequiredArgsConstructor
public class RappInstanceController {

    private final RappCacheService rappCacheService;
    private final RappInstanceStateMachine rappInstanceStateMachine;
    private final RappService rappService;

    @GetMapping
    public ResponseEntity<Map<UUID, RappInstance>> getAllRappInstances(@PathVariable("rapp_id") String rappId) {
        return rappCacheService.getRapp(rappId).map(Rapp::getRappInstances).map(ResponseEntity::ok).orElseThrow(
                () -> new RappHandlerException(HttpStatus.NOT_FOUND, "No instance found for rApp '" + rappId + "'."));
    }

    @PostMapping
    public ResponseEntity<RappInstance> createRappInstance(@PathVariable("rapp_id") String rappId,
            @RequestBody RappInstance rappInstance) {
        return rappCacheService.getRapp(rappId).map(rapp -> {
            rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
            rapp.getRappInstances().put(rappInstance.getRappInstanceId(), rappInstance);
            return ResponseEntity.ok(rappInstance);
        }).orElseThrow(() -> new RappHandlerException(HttpStatus.NOT_FOUND, "rApp '" + rappId + "' not found."));
    }

    @GetMapping("{rapp_instance_id}")
    public ResponseEntity<RappInstance> getRappInstance(@PathVariable("rapp_id") String rappId,
            @PathVariable("rapp_instance_id") UUID rappInstanceId) {
        return rappCacheService.getRapp(rappId).map(rapp -> Pair.of(rapp, rapp.getRappInstances().get(rappInstanceId)))
                       .filter(rappPair -> rappPair.getLeft().getRappInstances().containsKey(rappInstanceId))
                       .map(rappPair -> {
                           rappService.updateRappInstanceState(rappPair.getLeft(), rappPair.getRight());
                           RappInstance rappInstance = rappPair.getLeft().getRappInstances().get(rappInstanceId);
                           rappInstance.setState(rappInstanceStateMachine.getRappInstanceState(rappInstanceId));
                           return rappInstance;
                       }).map(ResponseEntity::ok).orElseThrow(() -> new RappHandlerException(HttpStatus.NOT_FOUND,
                        "No instance found for rApp '" + rappId + "'."));
    }

    @PutMapping("{rapp_instance_id}")
    public ResponseEntity<String> deployRappInstance(@PathVariable("rapp_id") String rappId,
            @PathVariable("rapp_instance_id") UUID rappInstanceId,
            @RequestBody RappInstanceDeployOrder rappInstanceDeployOrder) {
        //@formatter:off
        return rappCacheService.getRapp(rappId)
                   .filter(rapp -> rapp.getRappInstances().containsKey(rappInstanceId))
                   .map(rapp -> Pair.of(rapp, rapp.getRappInstances().get(rappInstanceId)))
                   .map(rappPair -> Optional.ofNullable(rappInstanceDeployOrder.getDeployOrder())
                        .filter(deployOrder -> deployOrder.equals(DeployOrder.DEPLOY))
                        .map(primeOrder -> rappService.deployRappInstance(rappPair.getLeft(), rappPair.getRight()))
                        .orElseGet(() -> rappService.undeployRappInstance(rappPair.getLeft(), rappPair.getRight())))
                   .orElseThrow(() -> new RappHandlerException(HttpStatus.NOT_FOUND,
                           "rApp instance '" + rappInstanceId + "' not found."));
        //@formatter:on
    }

    @DeleteMapping("{rapp_instance_id}")
    public ResponseEntity<Object> deleteRappInstance(@PathVariable("rapp_id") String rappId,
            @PathVariable("rapp_instance_id") UUID rappInstanceId) {
        return rappCacheService.getRapp(rappId).map(rapp -> Pair.of(rapp, rapp.getRappInstances()))
                       .filter(rappPair -> rappPair.getRight().containsKey(rappInstanceId) && rappPair.getRight()
                                                                                                      .get(rappInstanceId)
                                                                                                      .getState()
                                                                                                      .equals(RappInstanceState.UNDEPLOYED))
                       .map(rappPair -> {
                           rappInstanceStateMachine.deleteRappInstance(
                                   rappPair.getLeft().getRappInstances().get(rappInstanceId));
                           rappPair.getLeft().getRappInstances().remove(rappInstanceId);
                           return ResponseEntity.noContent().build();
                       }).orElseThrow(() -> new RappHandlerException(HttpStatus.NOT_FOUND,
                        "rApp instance '" + rappInstanceId + "' not found."));
    }

}
