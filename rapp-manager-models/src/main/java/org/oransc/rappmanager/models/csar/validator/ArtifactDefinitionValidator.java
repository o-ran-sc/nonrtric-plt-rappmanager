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
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.csar.RappCsarPathProvider;
import org.oransc.rappmanager.models.exception.RappValidationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ArtifactDefinitionValidator implements RappValidator {

    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;
    private final RappValidationUtils rappValidationUtils;
    String invalidAsdErrorMsg = "ASD definition in rApp package is invalid.";
    private static final int VALIDATION_ORDER = 15;

    @Override
    public int getOrder() {
        return VALIDATION_ORDER;
    }

    @Override
    public void validate(Object target, Errors errors) {
        MultipartFile multipartFile = (MultipartFile) target;
        String asdLocation = rappValidationUtils.getAsdDefinitionLocation(multipartFile);
        try {
            String asdContent = rappValidationUtils.getFileFromCsar(multipartFile, asdLocation).toString();
            JsonNode jsonNode = rappCsarConfigurationHandler.getAsdContentNode(asdContent);
            List<String> artifactFileList =
                    jsonNode.at(RappCsarPathProvider.ASD_ARTIFACTS_LOCATION_JSON_POINTER).findValuesAsText("file");
            artifactFileList.forEach(
                    artifactFile -> rappValidationUtils.isFileExistsInCsar(multipartFile, artifactFile));
        } catch (RappValidationException e) {
            throw new RappValidationException(e.getMessage());
        } catch (Exception e) {
            throw new RappValidationException(invalidAsdErrorMsg);
        }
    }
}
