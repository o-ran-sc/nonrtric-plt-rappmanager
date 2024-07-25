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

package org.oransc.rappmanager.models.csar;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RappCsarPathProvider {

    public static final String TOSCA_METADATA_LOCATION = "TOSCA-Metadata/TOSCA.meta";
    public static final String ENTRY_DEFINITIONS_INDEX = "Entry-Definitions";
    public static final String ACM_COMPOSITION_JSON_LOCATION = "Files/Acm/definition/compositions.json";
    public static final String ACM_DEFINITION_LOCATION = "Files/Acm/definition";
    public static final String ACM_INSTANCES_LOCATION = "Files/Acm/instances";
    public static final String SME_PROVIDER_FUNCS_LOCATION = "Files/Sme/providers";
    public static final String SME_SERVICE_APIS_LOCATION = "Files/Sme/serviceapis";
    public static final String SME_INVOKERS_LOCATION = "Files/Sme/invokers";
    public static final String DME_PRODUCER_INFO_TYPES_LOCATION = "Files/Dme/producerinfotypes";
    public static final String DME_CONSUMER_INFO_TYPES_LOCATION = "Files/Dme/consumerinfotypes";
    public static final String DME_INFO_PRODUCERS_LOCATION = "Files/Dme/infoproducers";
    public static final String DME_INFO_CONSUMERS_LOCATION = "Files/Dme/infoconsumers";
    public static final String ASD_LOCATION_JSON_POINTER =
            "/topology_template/node_templates/applicationServiceDescriptor";
    public static final String ASD_PROPERTIES_JSON_POINTER = ASD_LOCATION_JSON_POINTER + "/properties";
    public static final String ASD_ARTIFACTS_LOCATION_JSON_POINTER = ASD_LOCATION_JSON_POINTER + "/artifacts";
    public static final String ASD_DESCRIPTOR_JSON_POINTER = ASD_PROPERTIES_JSON_POINTER + "/descriptor_id";
    public static final String ASD_DESCRIPTOR_VARIANT_LOCATION_JSON_POINTER =
            ASD_PROPERTIES_JSON_POINTER + "/descriptor_invariant_id";
}
