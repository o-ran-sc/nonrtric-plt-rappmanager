/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.models.rappinstance.validator;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RappInstanceValidationHandler {

    private final List<RappInstanceValidator> rappInstanceValidators;

    public void validateRappInstance(Rapp rApp, RappInstance rAppInstance) {
        List<RappInstanceValidator> validatorList =
                rappInstanceValidators.stream().sorted(Comparator.comparing(RappInstanceValidator::getOrder)).toList();
        validatorList.forEach(rAppValidator -> rAppValidator.validate(rApp, rAppInstance));
    }
}
