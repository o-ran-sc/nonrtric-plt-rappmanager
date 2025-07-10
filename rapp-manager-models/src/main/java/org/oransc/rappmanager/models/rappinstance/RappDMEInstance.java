/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import jakarta.validation.GroupSequence;
import java.util.Set;
import lombok.Data;
import org.oransc.rappmanager.models.validators.AtleastOneFieldShouldNotBeNull;
import org.oransc.rappmanager.models.validators.ClassLevelValidatorGroup;
import org.oransc.rappmanager.models.validators.FieldLevelValidatorGroup;
import org.oransc.rappmanager.models.validators.NullOrNotEmpty;

@Data
@AtleastOneFieldShouldNotBeNull(fields = {"infoTypesProducer", "infoProducer", "infoTypeConsumer", "infoConsumer"},
        message = "At least one of the fields infoTypesProducer, infoProducer, infoTypeConsumer or infoConsumer must be provided",
        groups = ClassLevelValidatorGroup.class)
@GroupSequence({RappDMEInstance.class, ClassLevelValidatorGroup.class, FieldLevelValidatorGroup.class})
public class RappDMEInstance {

    @NullOrNotEmpty(groups = FieldLevelValidatorGroup.class)
    Set<String> infoTypesProducer;
    @NullOrNotEmpty(groups = FieldLevelValidatorGroup.class)
    String infoProducer;
    @NullOrNotEmpty(groups = FieldLevelValidatorGroup.class)
    String infoTypeConsumer;
    @NullOrNotEmpty(groups = FieldLevelValidatorGroup.class)
    String infoConsumer;
}
