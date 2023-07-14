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

package com.oransc.rappmanager.models;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RappCsarConfigurationHandler {

    Logger logger = LoggerFactory.getLogger(RappCsarConfigurationHandler.class);
    private final String acmInstantiationJsonLocation = "Files/Acm/instantiation.json";

    private final String smeProviderDomainLocation = "Files/Sme/provider-domain.json";

    private final String smeProviderApiLocation = "Files/Sme/provider-api.json";

    private final String smeInvokerLocation = "Files/Sme/invoker.json";


    public boolean isValidRappPackage(MultipartFile multipartFile) {
        return multipartFile.getOriginalFilename() != null && multipartFile.getOriginalFilename().endsWith(".csar")
                       && isFileExistsInCsar(multipartFile, acmInstantiationJsonLocation);
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

    public String getInstantiationPayload(Rapp rapp, UUID compositionId) {
        return getPayload(rapp, acmInstantiationJsonLocation).replaceAll("COMPOSITIONID",
                String.valueOf(compositionId));
    }

    String getPayload(Rapp rapp, String location) {
        File csarFile = new File(
                getRappPackageLocation(rapp.getPackageLocation(), rapp.getName(), rapp.getPackageName()).toUri());
        return getFileFromCsar(csarFile, location).toString();
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


    public String getSmeProviderDomainPayload(Rapp rapp) {
        return getPayload(rapp, smeProviderDomainLocation);
    }

    public String getSmeProviderApiPayload(Rapp rapp) {
        return getPayload(rapp, smeProviderApiLocation);
    }

    public String getSmeInvokerPayload(Rapp rapp) {
        return getPayload(rapp, smeInvokerLocation);
    }

}
