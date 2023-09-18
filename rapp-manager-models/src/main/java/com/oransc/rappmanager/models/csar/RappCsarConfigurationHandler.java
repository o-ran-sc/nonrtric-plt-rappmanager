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

package com.oransc.rappmanager.models.csar;

import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappResources;
import com.oransc.rappmanager.models.rappinstance.RappACMInstance;
import com.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RappCsarConfigurationHandler {

    Logger logger = LoggerFactory.getLogger(RappCsarConfigurationHandler.class);
    private static final String ACM_COMPOSITION_JSON_LOCATION = "Files/Acm/definition/compositions.json";
    private static final String ACM_DEFINITION_LOCATION = "Files/Acm/definition";
    private static final String ACM_INSTANCES_LOCATION = "Files/Acm/instances";

    private static final String SME_PROVIDER_FUNCS_LOCATION = "Files/Sme/providers";
    private static final String SME_SERVICE_APIS_LOCATION = "Files/Sme/serviceapis";

    private static final String SME_INVOKERS_LOCATION = "Files/Sme/invokers";


    public boolean isValidRappPackage(MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename != null) {
            return originalFilename.endsWith(".csar") && isFileExistsInCsar(multipartFile,
                    ACM_COMPOSITION_JSON_LOCATION);
        }
        return false;
    }

    boolean isFileExistsInCsar(MultipartFile multipartFile, String fileLocation) {
        try (ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(multipartFile.getInputStream())) {
            ArchiveEntry zipEntry;
            while ((zipEntry = zipArchiveInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().matches(fileLocation)) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        } catch (IOException e) {
            logger.error("Unable to find the CSAR file", e);
            return Boolean.FALSE;
        }
    }

    public Path getRappPackageLocation(String csarLocation, String rappId, String fileName) {
        return Path.of(csarLocation, rappId, fileName);
    }

    public String getInstantiationPayload(Rapp rapp, RappACMInstance rappACMInstance, UUID compositionId) {
        return getPayload(rapp, getResourceUri(ACM_INSTANCES_LOCATION, rappACMInstance.getInstance())).replaceAll(
                "COMPOSITIONID", String.valueOf(compositionId));
    }

    String getPayload(Rapp rapp, String location) {
        logger.info("Getting payload for {} from {}", rapp.getRappId(), location);
        File csarFile = getCsarFile(rapp);
        return getFileFromCsar(csarFile, location).toString();
    }

    File getCsarFile(Rapp rapp) {
        return new File(
                getRappPackageLocation(rapp.getPackageLocation(), rapp.getName(), rapp.getPackageName()).toUri());
    }

    ByteArrayOutputStream getFileFromCsar(File csarFile, String fileLocation) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (FileInputStream fileInputStream = new FileInputStream(csarFile);
             ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(fileInputStream)) {
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
            logger.error("Unable to find the CSAR file", e);
        }
        return byteArrayOutputStream;
    }


    public String getSmeProviderDomainPayload(Rapp rapp, RappSMEInstance rappSMEInstance) {
        return getPayload(rapp, getResourceUri(SME_PROVIDER_FUNCS_LOCATION, rappSMEInstance.getProviderFunction()));
    }

    public String getSmeProviderApiPayload(Rapp rapp, RappSMEInstance rappSMEInstance) {
        return getPayload(rapp, getResourceUri(SME_SERVICE_APIS_LOCATION, rappSMEInstance.getServiceApis()));
    }

    public String getSmeInvokerPayload(Rapp rapp, RappSMEInstance rappSMEInstance) {
        return getPayload(rapp, getResourceUri(SME_INVOKERS_LOCATION, rappSMEInstance.getInvokers()));
    }

    public String getAcmCompositionPayload(Rapp rapp) {
        return getPayload(rapp,
                getResourceUri(ACM_DEFINITION_LOCATION, rapp.getRappResources().getAcm().getCompositionDefinitions()));
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
                        getFileListFromCsar(csarFile, ACM_DEFINITION_LOCATION).iterator().next()).compositionInstances(
                        getFileListFromCsar(csarFile, ACM_INSTANCES_LOCATION)).build());
                rappResources.setSme(RappResources.SMEResources.builder().providerFunctions(
                                getFileListFromCsar(csarFile, SME_PROVIDER_FUNCS_LOCATION))
                                             .serviceApis(getFileListFromCsar(csarFile, SME_SERVICE_APIS_LOCATION))
                                             .invokers(getFileListFromCsar(csarFile, SME_INVOKERS_LOCATION)).build());
            }
        } catch (Exception e) {
            logger.warn("Error in getting the rapp resources", e);
        }
        return rappResources;
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
