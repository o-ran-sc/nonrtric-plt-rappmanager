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

import com.oransc.rappmanager.acm.service.AcmDeployer;
import com.oransc.rappmanager.configuration.RappManagerConfiguration;
import com.oransc.rappmanager.models.Rapp;
import com.oransc.rappmanager.models.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.RappEvent;
import com.oransc.rappmanager.models.RappState;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.statemachine.RappStateMachine;
import com.oransc.rappmanager.sme.service.SmeDeployer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "rapps")
@RequiredArgsConstructor
public class OnboardingController {

    Logger logger = LoggerFactory.getLogger(OnboardingController.class);
    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;
    private final AcmDeployer acmDeployer;
    private final SmeDeployer smeDeployer;
    private final RappManagerConfiguration rappManagerConfiguration;
    private final RappStateMachine rappStateMachine;
    private final RappCacheService rappCacheService;


    @GetMapping
    public ResponseEntity<Cache> getRapps() {
        return ResponseEntity.ok(rappCacheService.getAllRapp());
    }

    @GetMapping("{rapp_id}")
    public ResponseEntity<Rapp> getRapps(@PathVariable("rapp_id") String rappId) {
        Optional<Rapp> rappOptional = rappCacheService.getRapp(rappId);
        if (rappOptional.isPresent()) {
            acmDeployer.syncRappStatus(rappOptional.get());
            return ResponseEntity.ok(rappCacheService.getRapp(rappId).get());
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("{rapp_id}/onboard")
    public ResponseEntity<Object> uploadRappCsarFile(@PathVariable("rapp_id") String rappId,
            @RequestPart("file") MultipartFile csarFilePart) throws IOException {
        if (rappCsarConfigurationHandler.isValidRappPackage(csarFilePart)) {
            File csarFile = new File(
                    rappCsarConfigurationHandler.getRappPackageLocation(rappManagerConfiguration.getCsarLocation(),
                            rappId, csarFilePart.getOriginalFilename()).toUri());
            csarFile.getParentFile().mkdirs();
            Files.copy(csarFilePart.getInputStream(), csarFile.getAbsoluteFile().toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            Rapp rapp = Rapp.builder().name(rappId).packageLocation(rappManagerConfiguration.getCsarLocation())
                                .packageName(csarFile.getName()).state(RappState.ONBOARDED).build();
            rappCacheService.putRapp(rapp);
            rappStateMachine.onboardRapp(rapp.getRappId());
            return ResponseEntity.accepted().build();
        } else {
            logger.info("Invalid Rapp package for {}", rappId);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("{rapp_id}/deploy")
    public ResponseEntity<?> deployRapp(@PathVariable("rapp_id") String rappId) {
        Optional<Rapp> rappOptional = rappCacheService.getRapp(rappId);
        rappOptional.ifPresent(rapp -> {
            rappStateMachine.sendRappEvent(rapp, RappEvent.DEPLOYING);
        });
        if (rappOptional.isPresent() && acmDeployer.deployRapp(rappOptional.get()) && smeDeployer.deployRapp(
                rappOptional.get())) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.internalServerError().build();
    }

    @PostMapping("{rapp_id}/undeploy")
    public ResponseEntity<?> undeployRapp(@PathVariable("rapp_id") String rappId) {
        Optional<Rapp> rappOptional = rappCacheService.getRapp(rappId);
        rappOptional.ifPresent(rapp -> {
            rappStateMachine.sendRappEvent(rapp, RappEvent.UNDEPLOYING);
        });
        if (rappOptional.isPresent() && acmDeployer.undeployRapp(rappOptional.get()) && smeDeployer.undeployRapp(
                rappOptional.get())) {
            rappCacheService.deleteRapp(rappOptional.get());
            rappStateMachine.deleteRapp(rappOptional.get());
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping("info")
    public ResponseEntity<Object> getInfo() {
        return ResponseEntity.ok(acmDeployer.getAllParticipants());
    }

}
