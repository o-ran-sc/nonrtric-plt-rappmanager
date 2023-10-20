/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.oransc.participant.dme.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Util class for Test scope.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToscaUtils {

    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();
    private static final String TOSCA_TEMPLATE_YAML = "participant-dme.yaml";

    public static ToscaServiceTemplate readAutomationCompositionFromTosca() {
        return serializeAutomationCompositionYaml();
    }

    private static ToscaServiceTemplate serializeAutomationCompositionYaml() {
        String automationCompositionString = ResourceUtils.getResourceAsString(ToscaUtils.TOSCA_TEMPLATE_YAML);
        return yamlTranslator.fromYaml(automationCompositionString, ToscaServiceTemplate.class);
    }

    public static void main(String[] args) {
        System.out.println(serializeAutomationCompositionYaml());
    }
}
