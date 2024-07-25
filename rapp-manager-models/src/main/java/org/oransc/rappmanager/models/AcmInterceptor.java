/*-
 * ============LICENSE_START======================================================================
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

package org.oransc.rappmanager.models;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

public interface AcmInterceptor {

    String AC_DEFINITION_ELEMENT_NAME = "onap.policy.clamp.ac.element.AutomationCompositionDefinition";
    String AC_DEFINITION_ELEMENTS_INDEX = "elements";

    String AC_NODE_TYPE_ELEMENT_NAME = "org.onap.policy.clamp.acm.AutomationCompositionElement";
    String AC_NODE_TEMPLATE_PARTICIPANT_TYPE = "org.onap.policy.clamp.acm.Participant";
    String AC_TOSCA_DATA_TYPE_ROOT = "tosca.datatypes.Root";
    String TOSCA_IDENTIFIER_KEY = "onap.datatypes.ToscaConceptIdentifier";
    String TEMPLATE_PROVIDER = "NONRTRIC";

    String TOSCA_PROPERTY_TYPE_STRING = "string";
    String TOSCA_PROPERTY_TYPE_LIST = "list";

    default void injectToscaServiceTemplate(ToscaServiceTemplate toscaServiceTemplate, String acElementName,
            String acElementVersion) {
        toscaServiceTemplate.getDataTypes().putAll(getDataTypes());
        toscaServiceTemplate.getNodeTypes().putAll(getNodeTypes());
        toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().putAll(getNodeTemplates());

        Object o = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().get(AC_DEFINITION_ELEMENT_NAME)
                           .getProperties().get(AC_DEFINITION_ELEMENTS_INDEX);

        JsonArray elementJsonArray = JsonParser.parseString(o.toString()).getAsJsonArray();
        elementJsonArray.add(
                new Gson().toJsonTree(getElementToscaIdentifier(acElementName, acElementVersion)).getAsJsonObject());

        toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().get(AC_DEFINITION_ELEMENT_NAME)
                .getProperties().put(AC_DEFINITION_ELEMENTS_INDEX, elementJsonArray);
    }

    void injectToscaServiceTemplate(ToscaServiceTemplate toscaServiceTemplate);

    Map<String, ToscaNodeTemplate> getNodeTemplates();

    Map<String, ToscaNodeType> getNodeTypes();

    Map<String, ToscaDataType> getDataTypes();

    default ToscaConceptIdentifier getElementToscaIdentifier(String acElementName, String acElementVersion) {
        ToscaConceptIdentifier toscaConceptIdentifier = new ToscaConceptIdentifier();
        toscaConceptIdentifier.setName(acElementName);
        toscaConceptIdentifier.setVersion(acElementVersion);
        return toscaConceptIdentifier;
    }

    default void injectAutomationComposition(AutomationComposition automationComposition, Rapp rapp,
            RappInstance rappInstance) {
        automationComposition.getElements().putAll(getInstantiationElement(rapp, rappInstance));
    }

    Map<UUID, AutomationCompositionElement> getInstantiationElement(Rapp rapp, RappInstance rappInstance);
}
