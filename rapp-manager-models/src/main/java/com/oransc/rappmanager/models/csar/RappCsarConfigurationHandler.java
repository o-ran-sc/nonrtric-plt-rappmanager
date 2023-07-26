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
import com.oransc.rappmanager.models.rappinstance.RappACMInstance;
import com.oransc.rappmanager.models.rapp.RappResources;
import com.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RappCsarConfigurationHandler {

    Logger logger = LoggerFactory.getLogger(RappCsarConfigurationHandler.class);
    private final String acmCompositionJsonLocation = "Files/Acm/definition/compositions.json";
    private final String acmDefinitionLocation = "Files/Acm/definition";
    private final String acmInstancesLocation = "Files/Acm/instances";

    private final String smeProviderFuncsLocation = "Files/Sme/providers";
    private final String smeServiceApisLocation = "Files/Sme/serviceapis";

    private final String smeInvokersLocation = "Files/Sme/invokers";


    public boolean isValidRappPackage(MultipartFile multipartFile) {
        return multipartFile.getOriginalFilename() != null && multipartFile.getOriginalFilename().endsWith(".csar")
                       && isFileExistsInCsar(multipartFile, acmCompositionJsonLocation);
        //TODO Additional file checks needs to be added
    }

    boolean isFileExistsInCsar(MultipartFile multipartFile, String fileLocation) {
        try (ZipInputStream zipInputStream = new ZipInputStream(multipartFile.getInputStream())) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
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
        return getPayload(rapp, getResourceUri(acmInstancesLocation, rappACMInstance.getInstance())).replaceAll(
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
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(fileLocation)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
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
        return getPayload(rapp, getResourceUri(smeProviderFuncsLocation, rappSMEInstance.getProviderFunction()));
    }

    public String getSmeProviderApiPayload(Rapp rapp, RappSMEInstance rappSMEInstance) {
        return getPayload(rapp, getResourceUri(smeServiceApisLocation, rappSMEInstance.getServiceApis()));
    }

    public String getSmeInvokerPayload(Rapp rapp, RappSMEInstance rappSMEInstance) {
        return getPayload(rapp, getResourceUri(smeInvokersLocation, rappSMEInstance.getInvokers()));
    }

    public String getAcmCompositionPayload(Rapp rapp) {
        return getPayload(rapp,
                getResourceUri(acmDefinitionLocation, rapp.getRappResources().getAcm().getCompositionDefinitions()));
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
                                getFileListFromCsar(csarFile, acmDefinitionLocation).get(0))
                                             .compositionInstances(getFileListFromCsar(csarFile, acmInstancesLocation))
                                             .build());
                rappResources.setSme(RappResources.SMEResources.builder()
                                             .providerFunctions(getFileListFromCsar(csarFile, smeProviderFuncsLocation))
                                             .serviceApis(getFileListFromCsar(csarFile, smeServiceApisLocation))
                                             .invokers(getFileListFromCsar(csarFile, smeInvokersLocation)).build());
            }
        } catch (Exception e) {
            logger.warn("Error in getting the rapp resources", e);
        }
        return rappResources;
    }

    List<String> getFileListFromCsar(File csarFile, String dirLocation) {
        try (ZipFile zipFile = new ZipFile(csarFile)) {
            return zipFile.stream().filter(Predicate.not(ZipEntry::isDirectory)).map(ZipEntry::getName)
                           .filter(name -> name.startsWith(dirLocation))
                           .map(name -> name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."))).toList();
        } catch (IOException e) {
            logger.warn("Error in listing the files from csar", e);
        }
        return List.of();
    }
}
