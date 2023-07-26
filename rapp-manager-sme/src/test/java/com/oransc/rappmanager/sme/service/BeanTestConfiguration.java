package com.oransc.rappmanager.sme.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.sme.configuration.SmeConfiguration;
import com.oransc.rappmanager.sme.provider.rest.DefaultApiClient;
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

    private final SmeConfiguration smeConfiguration;

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

    @Bean("smeProviderApiClient")
    public com.oransc.rappmanager.sme.provider.ApiClient smeProviderApiClient(RestTemplate restTemplate) {
        return new com.oransc.rappmanager.sme.provider.ApiClient(restTemplate);
    }

    @Bean("smePublishServiceApiClient")
    public com.oransc.rappmanager.sme.publishservice.ApiClient smePublishServiceApiClient(RestTemplate restTemplate) {
        return new com.oransc.rappmanager.sme.publishservice.ApiClient(restTemplate);
    }

    @Bean("smeInvokerApiClient")
    public com.oransc.rappmanager.sme.invoker.ApiClient smeInvokerApiClient(RestTemplate restTemplate) {
        return new com.oransc.rappmanager.sme.invoker.ApiClient(restTemplate);
    }

    @Bean
    public DefaultApiClient defaultProviderApiClient(
            @Qualifier("smeProviderApiClient") com.oransc.rappmanager.sme.provider.ApiClient apiClient) {
        apiClient.setBasePath(smeConfiguration.getBaseUrl() + smeConfiguration.getProviderBasePath());
        return new DefaultApiClient(apiClient);
    }

    @Bean
    public com.oransc.rappmanager.sme.publishservice.rest.DefaultApiClient defaultPublishServiceApiClient(
            @Qualifier("smePublishServiceApiClient") com.oransc.rappmanager.sme.publishservice.ApiClient apiClient) {
        apiClient.setBasePath(smeConfiguration.getBaseUrl() + smeConfiguration.getPublishApiBasePath());
        return new com.oransc.rappmanager.sme.publishservice.rest.DefaultApiClient(apiClient);
    }

    @Bean
    public com.oransc.rappmanager.sme.invoker.rest.DefaultApiClient defaultInvokerApiClient(
            @Qualifier("smeInvokerApiClient") com.oransc.rappmanager.sme.invoker.ApiClient apiClient) {
        apiClient.setBasePath(smeConfiguration.getBaseUrl() + smeConfiguration.getInvokerBasePath());
        return new com.oransc.rappmanager.sme.invoker.rest.DefaultApiClient(apiClient);
    }
}
