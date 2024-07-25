/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

package org.oransc.rappmanager.models.rapp;

import java.util.Set;
import org.oransc.rappmanager.models.rappinstance.RappDMEInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstance;

public class RappDmeResourceBuilder {

    public RappResources getResources() {
        RappResources rappResources = new RappResources();
        RappResources.DMEResources dmeResources =
                new RappResources.DMEResources(Set.of("json-file-data-from-filestore"),
                        Set.of("xml-file-data-from-filestore"),
                        Set.of("json-file-data-producer", "xml-file-data-producer"),
                        Set.of("json-file-consumer", "xml-file-consumer"));
        rappResources.setDme(dmeResources);
        return rappResources;
    }

    public RappInstance getRappInstance() {
        RappInstance rappInstance = new RappInstance();
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypesProducer(Set.of("json-file-data-from-filestore"));
        rappDMEInstance.setInfoProducer("json-file-data-producer");
        rappDMEInstance.setInfoTypeConsumer("xml-file-data-from-filestore");
        rappDMEInstance.setInfoConsumer("json-file-consumer");
        rappInstance.setDme(rappDMEInstance);
        return rappInstance;
    }
}
