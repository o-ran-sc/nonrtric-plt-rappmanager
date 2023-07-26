package com.oransc.rappmanager.acm.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.acm.ApiClient;
import com.oransc.rappmanager.acm.configuration.ACMConfiguration;
import com.oransc.rappmanager.acm.rest.AutomationCompositionDefinitionApiClient;
import com.oransc.rappmanager.acm.rest.AutomationCompositionInstanceApiClient;
import com.oransc.rappmanager.acm.rest.ParticipantMonitoringApiClient;
import lombok.RequiredArgsConstructor;
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
