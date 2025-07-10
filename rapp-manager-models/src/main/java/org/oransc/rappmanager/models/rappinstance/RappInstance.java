/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.models.rappinstance;

import jakarta.validation.Valid;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Data;
import org.oransc.rappmanager.models.validators.AtleastOneFieldShouldNotBeNull;

@Data
@AtleastOneFieldShouldNotBeNull(fields = {"acm", "sme", "dme"},
        message = "At least one of the fields 'acm', 'sme', or 'dme' must not be null")
public class RappInstance {

    UUID rappInstanceId = UUID.randomUUID();
    RappInstanceState state = RappInstanceState.UNDEPLOYED;
    String reason;
    @Valid
    RappACMInstance acm;
    @Valid
    RappSMEInstance sme;
    @Valid
    RappDMEInstance dme;

    public boolean isSMEEnabled() {
        if (sme != null) {
            return Stream.of(sme.getInvokers(), sme.getServiceApis(), sme.getProviderFunction())
                           .anyMatch(smeResource -> smeResource != null && !smeResource.isEmpty());
        }
        return false;
    }

    public boolean isDMEEnabled() {
        if (dme != null) {
            return Stream.concat(
                            dme.getInfoTypesProducer() == null ? Stream.empty() : dme.getInfoTypesProducer().stream(),
                            Stream.of(dme.getInfoTypeConsumer(), dme.getInfoProducer(), dme.getInfoConsumer()))
                           .anyMatch(dmeResource -> dmeResource != null && !dmeResource.isEmpty());
        }
        return false;
    }
}
