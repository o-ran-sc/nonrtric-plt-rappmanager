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

package org.oransc.rappmanager.dme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;
import org.oransc.rappmanager.models.configuration.RappsEnvironmentConfiguration;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappDmeResourceBuilder;
import org.oransc.rappmanager.models.rapp.RappState;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {ObjectMapper.class, RappsEnvironmentConfiguration.class, RappCsarConfigurationHandler.class,
        DmeAcmInterceptor.class})
class DmeAcmInterceptorTest {

    @Autowired
    DmeAcmInterceptor dmeAcmInterceptor;

    RappDmeResourceBuilder rappDmeResourceBuilder = new RappDmeResourceBuilder();
    private static final String VALID_RAPP_FILE = "valid-rapp-package.csar";
    String validCsarFileLocation = "src/test/resources/";

    @ParameterizedTest
    @MethodSource("getrAppInstances")
    void testInjectAutomationComposition(RappInstance rAppInstance) {
        AutomationComposition automationComposition = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> elements =
                new HashMap<>(Map.of(UUID.randomUUID(), new AutomationCompositionElement()));
        automationComposition.setElements(elements);
        assertEquals(1, automationComposition.getElements().size());
        Rapp rApp = Rapp.builder().rappId(UUID.randomUUID()).name("").packageName(VALID_RAPP_FILE)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED)
                            .rappResources(rappDmeResourceBuilder.getResources()).build();
        dmeAcmInterceptor.injectAutomationComposition(automationComposition, rApp, rAppInstance);
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
        dmeAcmInterceptor.injectToscaServiceTemplate(toscaServiceTemplate);
        Object modObject =
                toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().get(nodeTemplateKey).getProperties()
                        .get(elements);
        JsonArray newElementArray = JsonParser.parseString(modObject.toString()).getAsJsonArray();
        assertEquals(4, toscaServiceTemplate.getDataTypes().size());
        assertEquals(2, toscaServiceTemplate.getNodeTypes().size());
        assertEquals(2, newElementArray.size());
    }

    private static Stream<Arguments> getrAppInstances() {
        RappDmeResourceBuilder rappDmeResourceBuilder = new RappDmeResourceBuilder();
        RappInstance rappInstanceProducerEmpty = rappDmeResourceBuilder.getRappInstance();
        RappInstance rappInstanceConsumerEmpty = rappDmeResourceBuilder.getRappInstance();
        RappInstance rappInstanceSameInfoType = rappDmeResourceBuilder.getRappInstance();
        rappInstanceProducerEmpty.getDme().setInfoTypesProducer(null);
        rappInstanceProducerEmpty.getDme().setInfoProducer(null);
        rappInstanceConsumerEmpty.getDme().setInfoTypeConsumer(null);
        rappInstanceConsumerEmpty.getDme().setInfoConsumer(null);
        Set<String> infoTypesProducer = new HashSet<>(rappInstanceSameInfoType.getDme().getInfoTypesProducer());
        infoTypesProducer.remove(rappInstanceSameInfoType.getDme().getInfoConsumer());
        rappInstanceSameInfoType.getDme().setInfoTypesProducer(infoTypesProducer);
        return Stream.of(Arguments.of(rappDmeResourceBuilder.getRappInstance()),
                Arguments.of(rappInstanceProducerEmpty), Arguments.of(rappInstanceConsumerEmpty),
                Arguments.of(rappInstanceSameInfoType));
    }
}
