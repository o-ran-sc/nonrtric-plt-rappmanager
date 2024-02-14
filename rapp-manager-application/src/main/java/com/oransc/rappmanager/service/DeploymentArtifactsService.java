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

package com.oransc.rappmanager.service;

import com.oransc.rappmanager.models.csar.DeploymentItem;
import com.oransc.rappmanager.models.csar.DeploymentItemArtifactType;
import com.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.exception.RappHandlerException;
import com.oransc.rappmanager.models.rapp.Rapp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class DeploymentArtifactsService {

    private final RestTemplate restTemplate;
    private final RappCsarConfigurationHandler rappCsarConfigurationHandler;

    public boolean configureDeploymentArtifacts(Rapp rapp) {
        List<DeploymentItem> deploymentItems = rappCsarConfigurationHandler.getDeploymentItems(rapp);
        return deploymentItems.stream().filter(deploymentItem -> deploymentItem.getArtifactType()
                                                                         .equals(DeploymentItemArtifactType.HELMCHART))
                       .allMatch(deploymentItem -> uploadHelmChart(rapp, deploymentItem));
    }

    boolean uploadHelmChart(Rapp rApp, DeploymentItem deploymentItem) throws RappHandlerException {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<ByteArrayResource> requestHttpEntity =
                    new HttpEntity<>(rappCsarConfigurationHandler.getArtifactPayload(rApp, deploymentItem.getFile()),
                            httpHeaders);
            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(deploymentItem.getTargetServerUri(), HttpMethod.POST, requestHttpEntity,
                            String.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return true;
            }
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
                return true;
            }
        } catch (Exception e) {
            throw new RappHandlerException(HttpStatus.BAD_REQUEST,
                    String.format("Unable to connect to the chartmuseum server %s to upload helm artifact %s",
                            deploymentItem.getTargetServerUri(), deploymentItem.getFile()));
        }
        return false;
    }
}
