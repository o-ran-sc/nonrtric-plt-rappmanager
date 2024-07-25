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

package org.oransc.rappmanager.models.csar.validator;

import org.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import org.oransc.rappmanager.models.csar.RappCsarPathProvider;
import org.oransc.rappmanager.models.exception.RappValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class RappValidationUtils {

    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;

    boolean isFileExistsInCsar(MultipartFile multipartFile, String fileLocation) {
        try (ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(multipartFile.getInputStream())) {
            ArchiveEntry zipEntry;
            while ((zipEntry = zipArchiveInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().matches(fileLocation)) {
                    return Boolean.TRUE;
                }
            }
            throw new RappValidationException("rApp package missing a file " + fileLocation);
        } catch (IOException e) {
            throw new RappValidationException("rApp package missing a file " + fileLocation);
        }
    }

    public String getAsdDefinitionLocation(final MultipartFile multipartFile) {
        return rappCsarConfigurationHandler.getAsdDefinitionLocation(
                getFileFromCsar(multipartFile, RappCsarPathProvider.TOSCA_METADATA_LOCATION).toString());
    }

    ByteArrayOutputStream getFileFromCsar(MultipartFile multipartFile, String fileLocation) {
        ByteArrayOutputStream byteArrayOutputStream;
        try (ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(multipartFile.getInputStream())) {
            byteArrayOutputStream = rappCsarConfigurationHandler.getFileFromCsar(zipArchiveInputStream, fileLocation);
        } catch (IOException e) {
            throw new RappValidationException(
                    String.format("Unable to get file %s from the multipart CSAR file", fileLocation));
        }
        return byteArrayOutputStream;
    }
}