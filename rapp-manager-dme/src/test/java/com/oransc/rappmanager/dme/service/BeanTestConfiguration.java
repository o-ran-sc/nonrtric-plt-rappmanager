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

package com.oransc.rappmanager.dme.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.dme.configuration.DmeConfiguration;
import com.oransc.rappmanager.dme.rest.DataConsumerApiClient;
import com.oransc.rappmanager.dme.rest.DataProducerRegistrationApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
@RequiredArgsConstructor
public class BeanTestConfiguration {

    private final DmeConfiguration dmeConfiguration;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
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
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public com.oransc.rappmanager.dme.ApiClient dmeApiClient(RestTemplate restTemplate) {
        com.oransc.rappmanager.dme.ApiClient apiClient = new com.oransc.rappmanager.dme.ApiClient(restTemplate);
        return apiClient.setBasePath(dmeConfiguration.getBaseUrl());
    }

    @Bean
    public DataProducerRegistrationApiClient dataProducerRegistrationApiClient(
            com.oransc.rappmanager.dme.ApiClient apiClient) {
        return new DataProducerRegistrationApiClient(apiClient);
    }

    @Bean
    public DataConsumerApiClient dataConsumerApiClient(com.oransc.rappmanager.dme.ApiClient apiClient) {
        return new DataConsumerApiClient(apiClient);
    }
}
