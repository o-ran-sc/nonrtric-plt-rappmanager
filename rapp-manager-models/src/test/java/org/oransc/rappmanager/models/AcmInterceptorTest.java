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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;

class AcmInterceptorTest implements AcmInterceptor {

    @Test
    void testInjectAutomationComposition() {
        AutomationComposition automationComposition = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> elements =
                new HashMap<>(Map.of(UUID.randomUUID(), new AutomationCompositionElement()));
        automationComposition.setElements(elements);
        assertEquals(1, automationComposition.getElements().size());
        injectAutomationComposition(automationComposition, mock(Rapp.class), mock(RappInstance.class));
        assertEquals(2, automationComposition.getElements().size());
    }

    @Test
    void testInjectToscaServiceTemplate() {
        ToscaServiceTemplate toscaServiceTemplate = new ToscaServiceTemplate();
        toscaServiceTemplate.setDataTypes(new HashMap<>(Map.of("datatype1", new ToscaDataType())));
        toscaServiceTemplate.setNodeTypes(new HashMap<>(Map.of("nodetype1", new ToscaNodeType())));
        ToscaTopologyTemplate toscaTopologyTemplate = new ToscaTopologyTemplate();
        ToscaNodeTemplate toscaNodeTemplate = new ToscaNodeTemplate();
        String elements = "elements";
        toscaNodeTemplate.setProperties(new HashMap<>(Map.of(elements, "[{}]")));
        String nodeTemplateKey = "onap.policy.clamp.ac.element.AutomationCompositionDefinition";

        toscaTopologyTemplate.setNodeTemplates(new HashMap<>(Map.of(nodeTemplateKey, toscaNodeTemplate)));
        toscaServiceTemplate.setToscaTopologyTemplate(toscaTopologyTemplate);
        Object o =
                toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().get(nodeTemplateKey).getProperties()
                        .get(elements);
        JsonArray elementArray = JsonParser.parseString(o.toString()).getAsJsonArray();
        assertEquals(1, toscaServiceTemplate.getDataTypes().size());
        assertEquals(1, toscaServiceTemplate.getNodeTypes().size());
        assertEquals(1, elementArray.size());
        injectToscaServiceTemplate(toscaServiceTemplate);
        Object modObject =
                toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().get(nodeTemplateKey).getProperties()
                        .get(elements);
        JsonArray newElementArray = JsonParser.parseString(modObject.toString()).getAsJsonArray();
        assertEquals(2, toscaServiceTemplate.getDataTypes().size());
        assertEquals(2, toscaServiceTemplate.getNodeTypes().size());
        assertEquals(2, newElementArray.size());
    }

    @Override
    public void injectToscaServiceTemplate(ToscaServiceTemplate toscaServiceTemplate) {
        injectToscaServiceTemplate(toscaServiceTemplate, "element1", "1.0.0");
    }

    @Override
    public Map<String, ToscaNodeTemplate> getNodeTemplates() {
        return new HashMap<>(Map.of(UUID.randomUUID().toString(), new ToscaNodeTemplate()));
    }

    @Override
    public Map<String, ToscaNodeType> getNodeTypes() {
        return new HashMap<>(Map.of(UUID.randomUUID().toString(), new ToscaNodeType()));
    }

    @Override
    public Map<String, ToscaDataType> getDataTypes() {
        return new HashMap<>(Map.of(UUID.randomUUID().toString(), new ToscaDataType()));
    }

    @Override
    public Map<UUID, AutomationCompositionElement> getInstantiationElement(Rapp rapp, RappInstance rappInstance) {
        return new HashMap<>(Map.of(UUID.randomUUID(), new AutomationCompositionElement()));
    }
}
