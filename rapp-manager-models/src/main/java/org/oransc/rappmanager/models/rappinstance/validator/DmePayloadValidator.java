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
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.stereotype.Component;

@Component
public class DmePayloadValidator implements RappInstanceValidator {

    private static final int VALIDATION_ORDER = 15;

    @Override
    public int getOrder() {
        return VALIDATION_ORDER;
    }

    @Override
    public void validate(@NotNull Rapp rApp, @NotNull RappInstance rAppInstance) {
        if (rAppInstance.isDMEEnabled()) {
            if (rAppInstance.getDme().getInfoTypesProducer() != null && !rApp.getRappResources().getDme()
                                                                                 .getProducerInfoTypes().containsAll(
                            rAppInstance.getDme().getInfoTypesProducer())) {
                throw new RappValidationException("Invalid DME info types producer in the rApp instance payload.");
            }

            comparePayloadValue(rAppInstance.getDme().getInfoProducer(),
                    rApp.getRappResources().getDme().getInfoProducers(),
                    "Invalid DME info producer in the rApp instance payload.");

            comparePayloadValue(rAppInstance.getDme().getInfoTypeConsumer(),
                    rApp.getRappResources().getDme().getConsumerInfoTypes(),
                    "Invalid DME info type consumer in the rApp instance payload.");

            comparePayloadValue(rAppInstance.getDme().getInfoConsumer(),
                    rApp.getRappResources().getDme().getInfoConsumers(),
                    "Invalid DME info consumer in the rApp instance payload.");
        }
    }
}
