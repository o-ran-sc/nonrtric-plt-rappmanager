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

import org.jetbrains.annotations.NotNull;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.stereotype.Component;

@Component
public class SmePayloadValidator implements RappInstanceValidator {

    private static final int VALIDATION_ORDER = 10;

    @Override
    public int getOrder() {
        return VALIDATION_ORDER;
    }

    @Override
    public void validate(@NotNull Rapp rApp, @NotNull RappInstance rAppInstance) {
        if (rAppInstance.isSMEEnabled()) {
            comparePayloadValue(rAppInstance.getSme().getProviderFunction(),
                    rApp.getRappResources().getSme().getProviderFunctions(),
                    "Invalid SME provider function in the rApp instance payload.");

            comparePayloadValue(rAppInstance.getSme().getServiceApis(),
                    rApp.getRappResources().getSme().getServiceApis(),
                    "Invalid SME service APIs in the rApp instance payload.");

            comparePayloadValue(rAppInstance.getSme().getInvokers(), rApp.getRappResources().getSme().getInvokers(),
                    "Invalid SME invokers in the rApp instance payload.");
        }
    }
}
