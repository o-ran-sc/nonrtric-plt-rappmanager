/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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
 */

package org.oransc.rappmanager.models.csar;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.Iterables.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.oransc.rappmanager.models.configuration.RappsEnvironmentConfiguration;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappResources;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
@RequiredArgsConstructor
public class RappCsarConfigurationHandler {

    Logger logger = LoggerFactory.getLogger(RappCsarConfigurationHandler.class);

    private final ObjectMapper objectMapper;
    private final RappsEnvironmentConfiguration rappsEnvironmentConfiguration;

    public Path getRappPackageLocation(String csarLocation, String rappId, String fileName) {
        return Path.of(csarLocation, rappId, fileName);
    }

    public String getInstantiationPayload(Rapp rapp, RappInstance rappInstance, UUID compositionId) {
        return getPayload(rapp, getResourceUri(RappCsarPathProvider.ACM_INSTANCES_LOCATION,
                rappInstance.getAcm().getInstance())).replace("DO_NOT_CHANGE_THIS_COMPOSITION_ID",
                        String.valueOf(compositionId))
                       .replace("DO_NOT_CHANGE_THIS_RAPP_INSTANCE_ID", String.valueOf(rappInstance.getRappInstanceId()))
                       .replace("DO_NOT_CHANGE_THIS_SME_DISCOVERY_ENDPOINT",
                               rappsEnvironmentConfiguration.getSmeDiscoveryEndpoint());
    }

    public ByteArrayResource getArtifactPayload(Rapp rapp, String location) {
        return new ByteArrayResource(getByteArrayStreamPayload(rapp, location).toByteArray());
    }

    String getPayload(Rapp rapp, String location) {
        return getByteArrayStreamPayload(rapp, location).toString();
    }

    ByteArrayOutputStream getByteArrayStreamPayload(Rapp rapp, String location) {
        logger.debug("Getting payload for {} from {}", rapp.getRappId(), location);
        File csarFile = getCsarFile(rapp);
        return getFileFromCsar(csarFile, location);
    }

    File getCsarFile(Rapp rapp) {
        return new File(
                getRappPackageLocation(rapp.getPackageLocation(), rapp.getName(), rapp.getPackageName()).toUri());
    }

    ByteArrayOutputStream getFileFromCsar(File csarFile, String fileLocation) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (FileInputStream fileInputStream = new FileInputStream(csarFile);
             ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(fileInputStream)) {
            byteArrayOutputStream = getFileFromCsar(zipArchiveInputStream, fileLocation);
        } catch (IOException e) {
            logger.info("Unable to get file {} from the CSAR file", fileLocation, e);
        }
        return byteArrayOutputStream;
    }

    public ByteArrayOutputStream getFileFromCsar(ZipArchiveInputStream zipArchiveInputStream, String fileLocation) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ArchiveEntry entry;
            while ((entry = zipArchiveInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(fileLocation)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipArchiveInputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException e) {
            logger.info("Unable to get file {} from the zip archive CSAR file", fileLocation, e);
        }
        return byteArrayOutputStream;
    }

    public JsonNode getAsdContentNode(String asdContent) throws JsonProcessingException {
        return objectMapper.readTree(new Gson().toJsonTree(new Yaml().load(asdContent)).toString());
    }

    String getAsdDefinitionLocation(final File csarFile) {
        return getAsdDefinitionLocation(
                getFileFromCsar(csarFile, RappCsarPathProvider.TOSCA_METADATA_LOCATION).toString());
    }

    public String getAsdDefinitionLocation(final String toscaMetadata) {
        String asdLocation = "";
        if (toscaMetadata != null && !toscaMetadata.isEmpty()) {
            final String entry = filter(on("\n").split(toscaMetadata),
                    line -> line.contains(RappCsarPathProvider.ENTRY_DEFINITIONS_INDEX)).iterator().next();
            asdLocation = entry.replace(RappCsarPathProvider.ENTRY_DEFINITIONS_INDEX + ":", "").trim();
        }
        return asdLocation;
    }


    public String getSmeProviderDomainPayload(Rapp rapp, RappSMEInstance rappSMEInstance) {
        return getPayload(rapp, getResourceUri(RappCsarPathProvider.SME_PROVIDER_FUNCS_LOCATION,
                rappSMEInstance.getProviderFunction()));
    }

    public String getSmeProviderApiPayload(Rapp rapp, RappSMEInstance rappSMEInstance) {
        return getPayload(rapp,
                getResourceUri(RappCsarPathProvider.SME_SERVICE_APIS_LOCATION, rappSMEInstance.getServiceApis()));
    }

    public String getSmeInvokerPayload(Rapp rapp, RappInstance rappInstance) {
        return getPayload(rapp, getResourceUri(RappCsarPathProvider.SME_INVOKERS_LOCATION,
                rappInstance.getSme().getInvokers())).replace("DO_NOT_CHANGE_THIS_RAPP_INSTANCE_ID",
                String.valueOf(rappInstance.getRappInstanceId()));
    }

    public String getAcmCompositionPayload(Rapp rapp) {
        return getPayload(rapp, getResourceUri(RappCsarPathProvider.ACM_DEFINITION_LOCATION,
                rapp.getRappResources().getAcm().getCompositionDefinitions()));
    }

    public String getDmeInfoProducerPayload(Rapp rapp, String producerIdentifier) {
        return getPayload(rapp, getResourceUri(RappCsarPathProvider.DME_INFO_PRODUCERS_LOCATION, producerIdentifier));
    }

    public String getDmeProducerInfoTypePayload(Rapp rapp, String infoTypeIdentifier) {
        return getPayload(rapp,
                getResourceUri(RappCsarPathProvider.DME_PRODUCER_INFO_TYPES_LOCATION, infoTypeIdentifier));
    }

    public String getDmeConsumerInfoTypePayload(Rapp rapp, String infoTypeIdentifier) {
        return getPayload(rapp,
                getResourceUri(RappCsarPathProvider.DME_CONSUMER_INFO_TYPES_LOCATION, infoTypeIdentifier));
    }

    public String getDmeInfoConsumerPayload(Rapp rapp, String infoConsumerIdentifier) {
        return getPayload(rapp,
                getResourceUri(RappCsarPathProvider.DME_INFO_CONSUMERS_LOCATION, infoConsumerIdentifier));
    }

    String getResourceUri(String resourceLocation, String resource) {
        return resourceLocation + "/" + resource + ".json";
    }

    public RappResources getRappResource(Rapp rapp) {
        RappResources rappResources = new RappResources();
        try {
            File csarFile = getCsarFile(rapp);
            if (csarFile.exists()) {
                rappResources.setAcm(RappResources.ACMResources.builder().compositionDefinitions(
                                getFileListFromCsar(csarFile, RappCsarPathProvider.ACM_DEFINITION_LOCATION).iterator().next())
                                             .compositionInstances(getFileListFromCsar(csarFile,
                                                     RappCsarPathProvider.ACM_INSTANCES_LOCATION)).build());
                rappResources.setSme(RappResources.SMEResources.builder().providerFunctions(
                        getFileListFromCsar(csarFile, RappCsarPathProvider.SME_PROVIDER_FUNCS_LOCATION)).serviceApis(
                        getFileListFromCsar(csarFile, RappCsarPathProvider.SME_SERVICE_APIS_LOCATION)).invokers(
                        getFileListFromCsar(csarFile, RappCsarPathProvider.SME_INVOKERS_LOCATION)).build());
                rappResources.setDme(RappResources.DMEResources.builder().producerInfoTypes(
                                getFileListFromCsar(csarFile, RappCsarPathProvider.DME_PRODUCER_INFO_TYPES_LOCATION))
                                             .consumerInfoTypes(getFileListFromCsar(csarFile,
                                                     RappCsarPathProvider.DME_CONSUMER_INFO_TYPES_LOCATION))
                                             .infoProducers(getFileListFromCsar(csarFile,
                                                     RappCsarPathProvider.DME_INFO_PRODUCERS_LOCATION)).infoConsumers(
                                getFileListFromCsar(csarFile, RappCsarPathProvider.DME_INFO_CONSUMERS_LOCATION))
                                             .build());
            }
        } catch (Exception e) {
            logger.warn("Error in getting the rapp resources", e);
        }
        return rappResources;
    }

    public AsdMetadata getAsdMetadata(Rapp rApp) {
        AsdMetadata asdMetadata = new AsdMetadata();
        File csarFile = getCsarFile(rApp);
        String asdDefinitionLocation = getAsdDefinitionLocation(csarFile);
        if (asdDefinitionLocation != null && !asdDefinitionLocation.isEmpty()) {
            try {
                String asdContent = getFileFromCsar(csarFile, asdDefinitionLocation).toString();
                if (asdContent != null && !asdContent.isEmpty()) {
                    JsonNode jsonNode = getAsdContentNode(asdContent);
                    JsonNode asdJsonNode = jsonNode.at(RappCsarPathProvider.ASD_LOCATION_JSON_POINTER);
                    asdMetadata = objectMapper.convertValue(asdJsonNode.at("/properties"), AsdMetadata.class);
                    asdMetadata.setDescription(asdJsonNode.at("/description").asText());

                    JsonNode artifactsJsonNode = jsonNode.at(RappCsarPathProvider.ASD_ARTIFACTS_LOCATION_JSON_POINTER);
                    final List<DeploymentItem> deploymentItems = new ArrayList<>();
                    artifactsJsonNode.forEach(artifactJsonNode -> {
                        DeploymentItem deploymentItem =
                                objectMapper.convertValue(artifactJsonNode.at("/properties"), DeploymentItem.class);
                        deploymentItem.setFile(artifactJsonNode.at("/file").asText());
                        deploymentItems.add(deploymentItem);
                    });
                    asdMetadata.setDeploymentItems(deploymentItems);
                }
            } catch (Exception e) {
                logger.warn("Unable to get the asd metadata items", e);
            }
        }
        return asdMetadata;
    }

    Set<String> getFileListFromCsar(File csarFile, String dirLocation) {
        try (ZipFile zipFile = new ZipFile(csarFile)) {
            return zipFile.stream().filter(Predicate.not(ZipEntry::isDirectory)).map(ZipEntry::getName)
                           .filter(name -> name.startsWith(dirLocation))
                           .map(name -> name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf(".")))
                           .collect(Collectors.toSet());
        } catch (IOException e) {
            logger.warn("Error in listing the files from csar", e);
        }
        return Set.of();
    }
}
