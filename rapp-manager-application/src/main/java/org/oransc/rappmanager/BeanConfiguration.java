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

package org.oransc.rappmanager;

import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.acm.ApiClient;
import org.oransc.rappmanager.acm.configuration.ACMConfiguration;
import org.oransc.rappmanager.acm.rest.AutomationCompositionDefinitionApiClient;
import org.oransc.rappmanager.acm.rest.AutomationCompositionInstanceApiClient;
import org.oransc.rappmanager.acm.rest.ParticipantMonitoringApiClient;
import org.oransc.rappmanager.dme.configuration.DmeConfiguration;
import org.oransc.rappmanager.dme.rest.DataConsumerApiClient;
import org.oransc.rappmanager.dme.rest.DataProducerRegistrationApiClient;
import org.oransc.rappmanager.sme.configuration.SmeConfiguration;
import org.oransc.rappmanager.sme.provider.rest.DefaultApiClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {

    private final ACMConfiguration acmConfiguration;
    private final SmeConfiguration smeConfiguration;
    private final DmeConfiguration dmeConfiguration;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean("acmApiClient")
    public ApiClient acmApiClient(RestTemplate restTemplate) {
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setUsername(acmConfiguration.getUsername());
        apiClient.setPassword(acmConfiguration.getPassword());
        return apiClient.setBasePath(acmConfiguration.getBaseUrl());
    }

    @Bean("smeProviderApiClient")
    public org.oransc.rappmanager.sme.provider.ApiClient smeProviderApiClient(RestTemplate restTemplate) {
        return new org.oransc.rappmanager.sme.provider.ApiClient(restTemplate);
    }

    @Bean("smePublishServiceApiClient")
    public org.oransc.rappmanager.sme.publishservice.ApiClient smePublishServiceApiClient(RestTemplate restTemplate) {
        return new org.oransc.rappmanager.sme.publishservice.ApiClient(restTemplate);
    }

    @Bean("smeInvokerApiClient")
    public org.oransc.rappmanager.sme.invoker.ApiClient smeInvokerApiClient(RestTemplate restTemplate) {
        return new org.oransc.rappmanager.sme.invoker.ApiClient(restTemplate);
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

    @Bean
    public DefaultApiClient defaultProviderApiClient(
            @Qualifier("smeProviderApiClient") org.oransc.rappmanager.sme.provider.ApiClient apiClient) {
        apiClient.setBasePath(smeConfiguration.getBaseUrl() + smeConfiguration.getProviderBasePath());
        return new DefaultApiClient(apiClient);
    }

    @Bean
    public org.oransc.rappmanager.sme.publishservice.rest.DefaultApiClient defaultPublishServiceApiClient(
            @Qualifier("smePublishServiceApiClient") org.oransc.rappmanager.sme.publishservice.ApiClient apiClient) {
        apiClient.setBasePath(smeConfiguration.getBaseUrl() + smeConfiguration.getPublishApiBasePath());
        return new org.oransc.rappmanager.sme.publishservice.rest.DefaultApiClient(apiClient);
    }

    @Bean
    public org.oransc.rappmanager.sme.invoker.rest.DefaultApiClient defaultInvokerApiClient(
            @Qualifier("smeInvokerApiClient") org.oransc.rappmanager.sme.invoker.ApiClient apiClient) {
        apiClient.setBasePath(smeConfiguration.getBaseUrl() + smeConfiguration.getInvokerBasePath());
        return new org.oransc.rappmanager.sme.invoker.rest.DefaultApiClient(apiClient);
    }

    @Bean
    public org.oransc.rappmanager.dme.ApiClient dmeApiClient(RestTemplate restTemplate) {
        org.oransc.rappmanager.dme.ApiClient apiClient = new org.oransc.rappmanager.dme.ApiClient(restTemplate);
        return apiClient.setBasePath(dmeConfiguration.getBaseUrl());
    }

    @Bean
    public DataProducerRegistrationApiClient dataProducerRegistrationApiClient(
            org.oransc.rappmanager.dme.ApiClient apiClient) {
        return new DataProducerRegistrationApiClient(apiClient);
    }

    @Bean
    public DataConsumerApiClient dataConsumerApiClient(org.oransc.rappmanager.dme.ApiClient apiClient) {
        return new DataConsumerApiClient(apiClient);
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

}
