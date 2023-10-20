/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.oransc.participant.dme;

import com.oransc.participant.dme.rest.DataConsumerApiClient;
import com.oransc.participant.dme.rest.DataProducerRegistrationApiClient;
import lombok.RequiredArgsConstructor;
import org.oransc.participant.dme.parameters.DmeParameters;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {

    private final DmeParameters dmeParameters;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public com.oransc.participant.dme.ApiClient dmeApiClient(RestTemplate restTemplate) {
        com.oransc.participant.dme.ApiClient apiClient = new com.oransc.participant.dme.ApiClient(restTemplate);
        return apiClient.setBasePath(dmeParameters.getBaseUrl());
    }

    @Bean
    public DataProducerRegistrationApiClient dataProducerRegistrationApiClient(
            com.oransc.participant.dme.ApiClient apiClient) {
        return new DataProducerRegistrationApiClient(apiClient);
    }

    @Bean
    public DataConsumerApiClient dataConsumerApiClient(com.oransc.participant.dme.ApiClient apiClient) {
        return new DataConsumerApiClient(apiClient);
    }
}
