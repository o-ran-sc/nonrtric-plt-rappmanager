/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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
 */

package org.oransc.rappmanager.models.rapp;


import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import org.oransc.rappmanager.models.csar.AsdMetadata;
import org.oransc.rappmanager.models.rappinstance.RappInstance;

@Data
@Builder(toBuilder = true)
public class Rapp {

    @Builder.Default
    UUID rappId = UUID.randomUUID();
    String name;
    RappState state;
    String reason;
    String packageLocation;
    String packageName;
    RappResources rappResources;
    AsdMetadata asdMetadata;
    @Builder.Default
    Map<UUID, RappInstance> rappInstances = new HashMap<>();

    UUID compositionId;

    @Transient
    public boolean isSMEEnabled() {
        if (rappResources != null && rappResources.getSme() != null) {
            return Stream.of(rappResources.getSme().getInvokers(), rappResources.getSme().getServiceApis(),
                    rappResources.getSme().getProviderFunctions()).anyMatch(smeResource -> !smeResource.isEmpty());
        }
        return false;

    }

    @Transient
    public boolean isDMEEnabled() {
        if (rappResources != null && rappResources.getDme() != null) {
            return Stream.of(rappResources.getDme().getProducerInfoTypes(),
                    rappResources.getDme().getConsumerInfoTypes(), rappResources.getDme().getInfoProducers(),
                    rappResources.getDme().getInfoConsumers()).anyMatch(smeResource -> !smeResource.isEmpty());
        }
        return false;
    }
}
