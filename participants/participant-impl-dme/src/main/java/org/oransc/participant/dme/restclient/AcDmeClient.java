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

package org.oransc.participant.dme.restclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.participant.dme.data.ConsumerJob;
import com.oransc.participant.dme.data.ProducerInfoTypeInfo;
import com.oransc.participant.dme.data.ProducerRegistrationInfo;
import com.oransc.participant.dme.rest.DataConsumerApiClient;
import com.oransc.participant.dme.rest.DataProducerRegistrationApiClient;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.oransc.participant.dme.exception.DmeException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcDmeClient {

    private final DataProducerRegistrationApiClient dataProducerRegistrationApiClient;
    private final DataConsumerApiClient dataConsumerApiClient;
    private final ObjectMapper objectMapper;

    public boolean isDmeHealthy() {
        return dataProducerRegistrationApiClient.getInfoTypdentifiersWithHttpInfo().getStatusCode().is2xxSuccessful();
    }

    public void createInfoType(Map<String, String> infoTypeMap) throws DmeException, JsonProcessingException {
        for (Map.Entry<String, String> entry : infoTypeMap.entrySet()) {
            String infoTypeName = entry.getKey();
            ProducerInfoTypeInfo infoTypeInfo = objectMapper.readValue(entry.getValue(), ProducerInfoTypeInfo.class);
            ResponseEntity<Object> objectResponseEntity =
                    dataProducerRegistrationApiClient.putInfoTypeWithHttpInfo(infoTypeName, infoTypeInfo);
            if (!objectResponseEntity.getStatusCode().is2xxSuccessful()) {
                throw new DmeException(objectResponseEntity.getStatusCode().value(), "Error in creating info types");
            }
        }
    }

    public void createDataProducer(Map<String, String> dataProducerMap) throws DmeException, JsonProcessingException {
        for (Map.Entry<String, String> entry : dataProducerMap.entrySet()) {
            String infoProducerName = entry.getKey();
            ProducerRegistrationInfo producerRegistrationInfo =
                    objectMapper.readValue(entry.getValue(), ProducerRegistrationInfo.class);
            ResponseEntity<Object> objectResponseEntity =
                    dataProducerRegistrationApiClient.putInfoProducerWithHttpInfo(infoProducerName,
                            producerRegistrationInfo);
            if (!objectResponseEntity.getStatusCode().is2xxSuccessful()) {
                throw new DmeException(objectResponseEntity.getStatusCode().value(), "Error in creating data producer");
            }
        }
    }

    public void createDataConsumer(Map<String, String> dataConsumerMap) throws DmeException, JsonProcessingException {
        for (Map.Entry<String, String> entry : dataConsumerMap.entrySet()) {
            String infoProducerName = entry.getKey();
            ConsumerJob consumerJob = objectMapper.readValue(entry.getValue(), ConsumerJob.class);
            ResponseEntity<Object> objectResponseEntity =
                    dataConsumerApiClient.putIndividualInfoJobWithHttpInfo(infoProducerName, consumerJob);
            if (!objectResponseEntity.getStatusCode().is2xxSuccessful()) {
                throw new DmeException(objectResponseEntity.getStatusCode().value(), "Error in creating data consumer");
            }
        }
    }

    public void deleteDataProducer(Set<String> dataProducerList) throws DmeException {
        for (String dataProducer : dataProducerList) {
            ResponseEntity<Object> objectResponseEntity =
                    dataProducerRegistrationApiClient.deleteInfoProducerWithHttpInfo(dataProducer);
            if (!objectResponseEntity.getStatusCode().is2xxSuccessful()) {
                throw new DmeException(objectResponseEntity.getStatusCode().value(), "Error in deleting data producer");
            }
        }
    }

    public void deleteDataConsumer(Set<String> dataConsumerList) throws DmeException {
        for (String dataConsumer : dataConsumerList) {
            ResponseEntity<Object> objectResponseEntity =
                    dataConsumerApiClient.deleteIndividualInfoJobWithHttpInfo(dataConsumer);
            if (!objectResponseEntity.getStatusCode().is2xxSuccessful()) {
                throw new DmeException(objectResponseEntity.getStatusCode().value(), "Error in deleting data consumer");
            }
        }
    }

}
