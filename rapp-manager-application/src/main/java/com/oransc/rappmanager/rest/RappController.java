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

import com.oransc.rappmanager.configuration.RappManagerConfiguration;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.rapp.PrimeOrder;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappPrimeOrder;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.service.RappService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "rapps")
@RequiredArgsConstructor
public class RappController {

    Logger logger = LoggerFactory.getLogger(RappController.class);
    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;
    private final RappManagerConfiguration rappManagerConfiguration;
    private final RappCacheService rappCacheService;
    private final RappService rappService;

    @GetMapping
    public ResponseEntity<Cache> getRapps() {
        return ResponseEntity.ok(rappCacheService.getAllRapp());
    }

    @GetMapping("{rapp_id}")
    public ResponseEntity<Rapp> getRapp(@PathVariable("rapp_id") String rappId) {
        return rappCacheService.getRapp(rappId).map(ResponseEntity::ok).orElse(ResponseEntity.badRequest().build());
    }

    @PostMapping("{rapp_id}")
    public ResponseEntity<Rapp> createRapp(@PathVariable("rapp_id") String rappId,
            @RequestPart("file") MultipartFile csarFilePart) throws IOException {
        if (rappCsarConfigurationHandler.isValidRappPackage(csarFilePart)) {
            File csarFile = new File(
                    rappCsarConfigurationHandler.getRappPackageLocation(rappManagerConfiguration.getCsarLocation(),
                            rappId, csarFilePart.getOriginalFilename()).toUri());
            csarFile.getParentFile().mkdirs();
            Files.copy(csarFilePart.getInputStream(), csarFile.getAbsoluteFile().toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            Rapp rapp = Rapp.builder().name(rappId).packageLocation(rappManagerConfiguration.getCsarLocation())
                                .packageName(csarFile.getName()).state(RappState.COMMISSIONED).build();
            rapp.setRappResources(rappCsarConfigurationHandler.getRappResource(rapp));
            rappCacheService.putRapp(rapp);
            return ResponseEntity.accepted().build();
        } else {
            logger.info("Invalid Rapp package for {}", rappId);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("{rapp_id}")
    public ResponseEntity<String> primeRapp(@PathVariable("rapp_id") String rappId,
            @RequestBody RappPrimeOrder rappPrimeOrder) {
        // @formatter:off
        return rappCacheService.getRapp(rappId)
                       .map(rapp -> Optional.ofNullable(rappPrimeOrder.getPrimeOrder())
                            .filter(primeOrder -> primeOrder.equals(PrimeOrder.PRIME))
                            .map(primeOrder -> rappService.primeRapp(rapp))
                            .orElseGet(() -> rappService.deprimeRapp(rapp)))
                       .orElse(ResponseEntity.notFound().build());
        // @formatter:on
    }

    @DeleteMapping("{rapp_id}")
    public ResponseEntity<Object> deleteRapp(@PathVariable("rapp_id") String rappId) {
        // @formatter:off
        return rappCacheService.getRapp(rappId)
               .filter(rapp -> rapp.getRappInstances().isEmpty() && rapp.getState().equals(RappState.COMMISSIONED))
               .map(rapp -> {
                   rappCacheService.deleteRapp(rapp);
                   return ResponseEntity.ok().build();
               })
               .orElse(ResponseEntity.notFound().build());
        // @formatter:on
    }
}
