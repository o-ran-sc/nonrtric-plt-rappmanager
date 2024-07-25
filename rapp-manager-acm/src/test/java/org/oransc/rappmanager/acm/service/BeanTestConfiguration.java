/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

package org.oransc.rappmanager.acm.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.acm.ApiClient;
import org.oransc.rappmanager.acm.configuration.ACMConfiguration;
import org.oransc.rappmanager.acm.configuration.JacksonMessageConverterConfiguration;
import org.oransc.rappmanager.acm.rest.AutomationCompositionDefinitionApiClient;
import org.oransc.rappmanager.acm.rest.AutomationCompositionInstanceApiClient;
import org.oransc.rappmanager.acm.rest.ParticipantMonitoringApiClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
@RequiredArgsConstructor
public class BeanTestConfiguration {

    private final ACMConfiguration acmConfiguration;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    public Gson gson() {
        return new Gson();
    }

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(); // or any other CacheManager implementation you want to use in the test
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        RestTemplate restTemplate = builder.build();
        restTemplate.getMessageConverters().add(new JacksonMessageConverterConfiguration(objectMapper));
        return restTemplate;
    }

    @Bean("acmApiClient")
    public ApiClient acmApiClient(RestTemplate restTemplate) {
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setUsername(acmConfiguration.getUsername());
        apiClient.setPassword(acmConfiguration.getPassword());
        return apiClient.setBasePath(acmConfiguration.getBaseUrl());
    }

    @Bean
    public ParticipantMonitoringApiClient participantMonitoringApiClient(
            @Qualifier("acmApiClient") ApiClient apiClient) {
        return new ParticipantMonitoringApiClient(apiClient);
    }

    @Bean
    public AutomationCompositionDefinitionApiClient automationCompositionDefinitionApiClient(
            @Qualifier("acmApiClient") ApiClient apiClient) {
        return new AutomationCompositionDefinitionApiClient(apiClient);
    }

    @Bean
    public AutomationCompositionInstanceApiClient automationCompositionInstanceApiClient(
            @Qualifier("acmApiClient") ApiClient apiClient) {
        return new AutomationCompositionInstanceApiClient(apiClient);
    }


}
