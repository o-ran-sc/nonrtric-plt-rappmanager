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

package org.oransc.rappmanager.dme.service;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaSchemaDefinition;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.oransc.rappmanager.dme.models.DataConsumerEntity;
import org.oransc.rappmanager.dme.models.DataProducerEntity;
import org.oransc.rappmanager.dme.models.InfoTypeEntity;
import org.oransc.rappmanager.models.AcmInterceptor;
import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DmeAcmInterceptor implements AcmInterceptor {

    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;

    String dmeInfoTypeEntity = "org.onap.datatypes.policy.clamp.acm.DMEAutomationCompositionElement.InfoTypeEntity";
    String version100 = "1.0.0";
    String version101 = "1.0.1";
    String dmeDataProducerEntity =
            "org.onap.datatypes.policy.clamp.acm.DMEAutomationCompositionElement.DataProducerEntity";
    String dmeDataConsumerEntity =
            "org.onap.datatypes.policy.clamp.acm.DMEAutomationCompositionElement.DataConsumerEntity";
    String infoTypeEntitiesKey = "infoTypeEntities";
    String dataProducerEntitiesKey = "dataProducerEntities";
    String dataConsumerEntitiesKey = "dataConsumerEntities";
    String toscaServiceTemplateDmeElement = "onap.policy.clamp.ac.element.DMEAutomationCompositionElement";
    String toscaServiceTemplateDmeElementVersion = "1.2.3";
    String toscaNodeTypeDmeElement = "org.onap.policy.clamp.acm.DMEAutomationCompositionElement";
    String payload = "payload";

    @Override
    public void injectToscaServiceTemplate(ToscaServiceTemplate toscaServiceTemplate) {
        injectToscaServiceTemplate(toscaServiceTemplate, toscaServiceTemplateDmeElement,
                toscaServiceTemplateDmeElementVersion);
    }

    @Override
    public Map<String, ToscaNodeTemplate> getNodeTemplates() {
        Map<String, ToscaNodeTemplate> toscaNodeTemplateMap = new HashMap<>();

        ToscaNodeTemplate nodeTemplateParticipant = getNodeTemplateParticipant();
        toscaNodeTemplateMap.put("org.onap.policy.clamp.acm.DMEAutomationCompositionParticipant",
                nodeTemplateParticipant);
        ToscaNodeTemplate dmeAcmElement = getDmeAcmElement();
        toscaNodeTemplateMap.put(toscaServiceTemplateDmeElement, dmeAcmElement);

        return toscaNodeTemplateMap;
    }

    @Override
    public Map<String, ToscaNodeType> getNodeTypes() {
        return Map.of(toscaNodeTypeDmeElement, getNodeType());
    }

    @Override
    public Map<String, ToscaDataType> getDataTypes() {
        Map<String, ToscaDataType> toscaDataTypeMap = new HashMap<>();

        ToscaDataType dmeInfoTypeDataType = getDmeInfoTypeDataType();
        toscaDataTypeMap.put(dmeInfoTypeEntity, dmeInfoTypeDataType);

        ToscaDataType dmeDataProducerDataType = getDmeDataProducerDataType();
        toscaDataTypeMap.put(dmeDataProducerEntity, dmeDataProducerDataType);

        ToscaDataType dmeDataConsumerDataType = getDmeDataConsumerDataType();
        toscaDataTypeMap.put(dmeDataConsumerEntity, dmeDataConsumerDataType);

        return toscaDataTypeMap;
    }

    @Override
    public Map<UUID, AutomationCompositionElement> getInstantiationElement(Rapp rapp, RappInstance rappInstance) {
        ToscaConceptIdentifier toscaConceptIdentifier = new ToscaConceptIdentifier();
        toscaConceptIdentifier.setName(toscaServiceTemplateDmeElement);
        toscaConceptIdentifier.setVersion(toscaServiceTemplateDmeElementVersion);

        AutomationCompositionElement automationCompositionElement = new AutomationCompositionElement();
        automationCompositionElement.setDefinition(toscaConceptIdentifier);

        Map<String, Object> properties = new HashMap<>();
        properties.put(infoTypeEntitiesKey, getInfoTypeEntities(rapp, rappInstance));
        properties.put(dataProducerEntitiesKey, getDataProducerEntities(rapp, rappInstance));
        properties.put(dataConsumerEntitiesKey, getDataConsumerEntities(rapp, rappInstance));

        automationCompositionElement.setProperties(properties);

        return Map.of(automationCompositionElement.getId(), automationCompositionElement);
    }

    private List<InfoTypeEntity> getInfoTypeEntities(Rapp rapp, RappInstance rappInstance) {
        List<InfoTypeEntity> infoTypeEntityList = new ArrayList<>();
        if (rappInstance.getDme().getInfoTypeConsumer() != null) {
            String dmeConsumerInfoTypePayload = rappCsarConfigurationHandler.getDmeConsumerInfoTypePayload(rapp,
                    rappInstance.getDme().getInfoTypeConsumer());
            infoTypeEntityList.add(InfoTypeEntity.builder().infoTypeEntityId(
                            new ToscaConceptIdentifier(rappInstance.getDme().getInfoTypeConsumer(), version101))
                                           .infoTypeId(rappInstance.getDme().getInfoTypeConsumer())
                                           .payload(JsonParser.parseString(dmeConsumerInfoTypePayload).toString())
                                           .build());
        }


        if (rappInstance.getDme().getInfoTypesProducer() != null) {
            rappInstance.getDme().getInfoTypesProducer().forEach(infoTypeProducer -> {
                if (!infoTypeProducer.equals(rappInstance.getDme().getInfoTypeConsumer())) {
                    String dmeProducerInfoTypePayload =
                            rappCsarConfigurationHandler.getDmeProducerInfoTypePayload(rapp, infoTypeProducer);
                    infoTypeEntityList.add(InfoTypeEntity.builder().infoTypeEntityId(
                                    new ToscaConceptIdentifier(infoTypeProducer, version101)).infoTypeId(infoTypeProducer)
                                                   .payload(JsonParser.parseString(dmeProducerInfoTypePayload)
                                                                    .toString()).build());
                }
            });
        }
        return infoTypeEntityList;
    }

    List<DataProducerEntity> getDataProducerEntities(Rapp rapp, RappInstance rappInstance) {
        List<DataProducerEntity> dataProducerEntityList = new ArrayList<>();
        if (rappInstance.getDme().getInfoProducer() != null) {
            String dmeInfoProducerPayload = rappCsarConfigurationHandler.getDmeInfoProducerPayload(rapp,
                    rappInstance.getDme().getInfoProducer());
            dataProducerEntityList.add(DataProducerEntity.builder().dataProducerEntityId(
                            new ToscaConceptIdentifier(rappInstance.getDme().getInfoProducer(), version101))
                                               .dataProducerId(rappInstance.getDme().getInfoProducer())
                                               .payload(JsonParser.parseString(dmeInfoProducerPayload).toString())
                                               .build());
        }
        return dataProducerEntityList;
    }

    List<DataConsumerEntity> getDataConsumerEntities(Rapp rapp, RappInstance rappInstance) {
        List<DataConsumerEntity> dataConsumerEntityList = new ArrayList<>();
        if (rappInstance.getDme().getInfoConsumer() != null) {
            String dmeInfoConsumerPayload = rappCsarConfigurationHandler.getDmeInfoConsumerPayload(rapp,
                    rappInstance.getDme().getInfoConsumer());
            dataConsumerEntityList.add(DataConsumerEntity.builder().dataConsumerEntityId(
                            new ToscaConceptIdentifier(rappInstance.getDme().getInfoConsumer(), version101))
                                               .dataConsumerId(rappInstance.getDme().getInfoConsumer())
                                               .payload(JsonParser.parseString(dmeInfoConsumerPayload).toString())
                                               .build());
        }
        return dataConsumerEntityList;
    }

    ToscaNodeTemplate getDmeAcmElement() {
        ToscaNodeTemplate toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setVersion("1.2.3");
        toscaNodeTemplate.setType(toscaNodeTypeDmeElement);
        toscaNodeTemplate.setTypeVersion(version101);
        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put("provider", TEMPLATE_PROVIDER);
        propertiesMap.put("participantType",
                new ToscaConceptIdentifier("org.onap.policy.clamp.acm.DMEParticipant", "2.3.4"));
        toscaNodeTemplate.setProperties(propertiesMap);
        return toscaNodeTemplate;
    }

    ToscaNodeTemplate getNodeTemplateParticipant() {
        ToscaNodeTemplate toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setVersion("2.3.4");
        toscaNodeTemplate.setType(AC_NODE_TEMPLATE_PARTICIPANT_TYPE);
        toscaNodeTemplate.setTypeVersion(version101);
        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put("provider", TEMPLATE_PROVIDER);
        toscaNodeTemplate.setProperties(propertiesMap);
        return toscaNodeTemplate;
    }

    ToscaDataType getDmeDataConsumerDataType() {
        ToscaDataType toscaDataType = new ToscaDataType();
        toscaDataType.setVersion(version100);
        toscaDataType.setDerivedFrom(AC_TOSCA_DATA_TYPE_ROOT);

        Map<String, ToscaProperty> propertyMap = new HashMap<>();
        ToscaProperty dataConsumerEntityIdProperty = getToscaProperty(TOSCA_IDENTIFIER_KEY);
        propertyMap.put("dataConsumerEntityId", dataConsumerEntityIdProperty);
        ToscaProperty dataConsumerIdProperty = getToscaProperty(TOSCA_PROPERTY_TYPE_STRING);
        propertyMap.put("dataConsumerId", dataConsumerIdProperty);
        ToscaProperty payloadProperty = getToscaProperty(TOSCA_PROPERTY_TYPE_STRING);
        propertyMap.put(payload, payloadProperty);
        toscaDataType.setProperties(propertyMap);

        return toscaDataType;
    }


    ToscaDataType getDmeDataProducerDataType() {
        ToscaDataType toscaDataType = new ToscaDataType();
        toscaDataType.setVersion(version100);
        toscaDataType.setDerivedFrom(AC_TOSCA_DATA_TYPE_ROOT);

        Map<String, ToscaProperty> propertyMap = new HashMap<>();
        ToscaProperty dataProducerEntityIdProperty = getToscaProperty(TOSCA_IDENTIFIER_KEY);
        propertyMap.put("dataProducerEntityId", dataProducerEntityIdProperty);
        ToscaProperty dataProducerIdProperty = getToscaProperty(TOSCA_PROPERTY_TYPE_STRING);
        propertyMap.put("dataProducerId", dataProducerIdProperty);
        ToscaProperty payloadProperty = getToscaProperty(TOSCA_PROPERTY_TYPE_STRING);
        propertyMap.put(payload, payloadProperty);
        toscaDataType.setProperties(propertyMap);

        return toscaDataType;
    }

    ToscaDataType getDmeInfoTypeDataType() {
        ToscaDataType toscaDataType = new ToscaDataType();
        toscaDataType.setVersion(version100);
        toscaDataType.setDerivedFrom(AC_TOSCA_DATA_TYPE_ROOT);

        Map<String, ToscaProperty> propertyMap = new HashMap<>();
        ToscaProperty infoTypeEntityIdProperty = getToscaProperty(TOSCA_IDENTIFIER_KEY);
        propertyMap.put("infoTypeEntityId", infoTypeEntityIdProperty);
        ToscaProperty infoTypeIdProperty = getToscaProperty(TOSCA_PROPERTY_TYPE_STRING);
        propertyMap.put("infoTypeId", infoTypeIdProperty);
        ToscaProperty payloadProperty = getToscaProperty(TOSCA_PROPERTY_TYPE_STRING);
        propertyMap.put(payload, payloadProperty);
        toscaDataType.setProperties(propertyMap);

        return toscaDataType;
    }

    ToscaProperty getToscaProperty(String type) {
        ToscaProperty infoTypeEntityIdProperty = new ToscaProperty();
        infoTypeEntityIdProperty.setType(type);
        infoTypeEntityIdProperty.setRequired(true);
        return infoTypeEntityIdProperty;
    }

    ToscaNodeType getNodeType() {
        ToscaNodeType toscaNodeType = new ToscaNodeType();
        toscaNodeType.setVersion(version101);
        toscaNodeType.setDerivedFrom(AC_NODE_TYPE_ELEMENT_NAME);
        toscaNodeType.setProperties(Map.of(infoTypeEntitiesKey, getInfoTypeProperties(), dataProducerEntitiesKey,
                getDataProducerProperties(), dataConsumerEntitiesKey, getDataConsumerProperties()));
        return toscaNodeType;
    }

    ToscaProperty getInfoTypeProperties() {
        return getToscoProperty(dmeInfoTypeEntity, version100);
    }

    ToscaProperty getDataProducerProperties() {
        return getToscoProperty(dmeDataProducerEntity, version100);
    }

    ToscaProperty getDataConsumerProperties() {
        return getToscoProperty(dmeDataConsumerEntity, version100);
    }

    ToscaProperty getToscoProperty(String schemaType, String schemaTypeVersion) {
        ToscaProperty toscaProperty = new ToscaProperty();
        toscaProperty.setType(TOSCA_PROPERTY_TYPE_LIST);
        toscaProperty.setRequired(true);
        ToscaSchemaDefinition toscaSchemaDefinition = new ToscaSchemaDefinition();
        toscaSchemaDefinition.setType(schemaType);
        toscaSchemaDefinition.setTypeVersion(schemaTypeVersion);
        toscaProperty.setEntrySchema(toscaSchemaDefinition);
        return toscaProperty;
    }
}
