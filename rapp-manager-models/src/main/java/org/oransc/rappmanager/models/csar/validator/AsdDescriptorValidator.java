/*
 * ============LICENSE_START======================================================================
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
 *
 */

package org.oransc.rappmanager.models.csar.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.oransc.rappmanager.models.cache.RappCacheService;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.csar.RappCsarPathProvider;
import org.oransc.rappmanager.models.exception.RappValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class AsdDescriptorValidator implements RappValidator {

    private final RappCacheService rappCacheService;
    private final RappValidationUtils rappValidationUtils;
    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;
    String invalidAsdErrorMsg = "ASD definition in rApp package is invalid.";

    private static final int VALIDATION_ORDER = 10;

    @Override
    public int getOrder() {
        return VALIDATION_ORDER;
    }

    @Override
    public void validate(Object target, Errors errors) {
        MultipartFile multipartFile = (MultipartFile) target;
        String asdLocation = rappValidationUtils.getAsdDefinitionLocation(multipartFile);
        if (asdLocation != null && !asdLocation.isEmpty() && rappValidationUtils.isFileExistsInCsar(multipartFile,
                asdLocation)) {
            try {
                String asdContent = rappValidationUtils.getFileFromCsar(multipartFile, asdLocation).toString();
                if (asdContent != null && !asdContent.isEmpty()) {
                    JsonNode jsonNode = rappCsarConfigurationHandler.getAsdContentNode(asdContent);
                    checkAsdDescriptorExists(jsonNode.at(RappCsarPathProvider.ASD_DESCRIPTOR_JSON_POINTER).asText());
                    checkAsdDescriptorVariantExists(
                            jsonNode.at(RappCsarPathProvider.ASD_DESCRIPTOR_VARIANT_LOCATION_JSON_POINTER).asText());
                } else {
                    throw new RappValidationException(invalidAsdErrorMsg);
                }
            } catch (RappValidationException e) {
                throw new RappValidationException(e.getMessage());
            } catch (Exception e) {
                throw new RappValidationException(invalidAsdErrorMsg);
            }
        } else {
            throw new RappValidationException(invalidAsdErrorMsg);
        }
    }

    boolean checkAsdDescriptorExists(String descriptorId) {
        if (rappCacheService.getAllRapp().stream()
                    .anyMatch(rapp -> rapp.getAsdMetadata().getDescriptorId().equals(descriptorId))) {
            throw new RappValidationException("ASD descriptor already exists.");
        }
        return true;
    }

    boolean checkAsdDescriptorVariantExists(String descriptorVariantId) {
        if (rappCacheService.getAllRapp().stream()
                    .anyMatch(rapp -> rapp.getAsdMetadata().getDescriptorInvariantId().equals(descriptorVariantId))) {
            throw new RappValidationException("ASD descriptor invariant already exists.");
        }
        return true;
    }
}
