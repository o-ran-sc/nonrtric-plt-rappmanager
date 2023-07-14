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

package com.oransc.rappmanager.models;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder(toBuilder = true)
public class Rapp {

    @Builder.Default
    UUID rappId = UUID.randomUUID();
    String name;
    RappState state;
    String packageLocation;
    String packageName;
    UUID compositionId;
    UUID compositionInstanceId;
    String smeApfId;
    String smeAefId;
    @Builder.Default
    Map<String, String> smeProviderFunctions = new HashMap<>();
    @Builder.Default
    List<String> smeServiceApis = new ArrayList<>();
    @Builder.Default
    List<String> smeInvokers = new ArrayList<>();
}
