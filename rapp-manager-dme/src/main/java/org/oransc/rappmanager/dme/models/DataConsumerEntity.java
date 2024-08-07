/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.
 * Copyright (C) 2023-2024 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.dme.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@Data
@Builder
public class DataConsumerEntity {

    @NotNull
    @JsonIgnore
    private ToscaConceptIdentifier dataConsumerEntityId;

    @NotNull
    private String dataConsumerId;

    @NotNull
    private String payload;

}
