/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.participant.dme.rest.DataConsumerApiClient;
import com.oransc.participant.dme.rest.DataProducerRegistrationApiClient;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.oransc.participant.dme.exception.DmeException;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AcA1PmsClientTest {

    @MockitoBean
    DataProducerRegistrationApiClient dataProducerRegistrationApiClient;

    @MockitoBean
    DataConsumerApiClient dataConsumerApiClient;

    @MockitoBean
    AcDmeClient acDmeClient;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void initialize() {
        acDmeClient = new AcDmeClient(dataProducerRegistrationApiClient, dataConsumerApiClient, objectMapper);
    }

    @Test
    void testHealthyDme() {
        when(dataProducerRegistrationApiClient.getInfoTypdentifiersWithHttpInfo()).thenReturn(
                ResponseEntity.ok().build());
        assertTrue(acDmeClient.isDmeHealthy());
    }

    @Test
    void testUnhealthyDme() {
        when(dataProducerRegistrationApiClient.getInfoTypdentifiersWithHttpInfo()).thenReturn(
                ResponseEntity.internalServerError().build());
        assertFalse(acDmeClient.isDmeHealthy());
    }

    @ParameterizedTest
    @MethodSource("getInfoTypes")
    void testcreateInfoTypeSuccess(Map<String, String> infoTypeMap) {
        when(dataProducerRegistrationApiClient.putInfoTypeWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build());
        assertDoesNotThrow(() -> acDmeClient.createInfoType(infoTypeMap));
    }

    @Test
    void testcreateInfoTypeFailure() {
        when(dataProducerRegistrationApiClient.putInfoTypeWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.internalServerError().build());
        assertThrows(DmeException.class, () -> acDmeClient.createInfoType(Map.of("infotype1", "{}")));
    }

    @ParameterizedTest
    @MethodSource("getDataProducers")
    void testcreateDataProducerSuccess(Map<String, String> dataProducerMap) {
        when(dataProducerRegistrationApiClient.putInfoProducerWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build());
        assertDoesNotThrow(() -> acDmeClient.createDataProducer(dataProducerMap));
    }


    @Test
    void testcreateDataProducerFailure() {
        when(dataProducerRegistrationApiClient.putInfoProducerWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.internalServerError().build());
        assertThrows(DmeException.class, () -> acDmeClient.createDataProducer(Map.of("producer1", "{}")));
    }

    @ParameterizedTest
    @MethodSource("getDataConsumers")
    void testcreateDataConsumerSuccess(Map<String, String> dataConsumerMap) {
        when(dataConsumerApiClient.putIndividualInfoJobWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build());
        assertDoesNotThrow(() -> acDmeClient.createDataConsumer(dataConsumerMap));
    }

    @Test
    void testcreateDataConsumerFailure() {
        when(dataConsumerApiClient.putIndividualInfoJobWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.internalServerError().build());
        assertThrows(DmeException.class, () -> acDmeClient.createDataConsumer(Map.of("consumer1", "{}")));
    }

    @ParameterizedTest
    @MethodSource("getDeleteDataProducers")
    void testDeleteDataProducerSuccess() {
        when(dataProducerRegistrationApiClient.deleteInfoProducerWithHttpInfo(any())).thenReturn(
                ResponseEntity.ok().build());
        assertDoesNotThrow(() -> acDmeClient.deleteDataProducer(Set.of("producer1")));
    }

    @Test
    void testDeleteDataProducerFailure() {
        when(dataProducerRegistrationApiClient.deleteInfoProducerWithHttpInfo(any())).thenReturn(
                ResponseEntity.internalServerError().build());
        assertThrows(DmeException.class, () -> acDmeClient.deleteDataProducer(Set.of("producer1")));
    }

    @ParameterizedTest
    @MethodSource("getDeleteDataConsumers")
    void testDeleteDataConsumerSuccess() {
        when(dataConsumerApiClient.deleteIndividualInfoJobWithHttpInfo(any())).thenReturn(ResponseEntity.ok().build());
        assertDoesNotThrow(() -> acDmeClient.deleteDataConsumer(Set.of("consumer1")));
    }

    @Test
    void testDeleteDataConsumerFailure() {
        when(dataConsumerApiClient.deleteIndividualInfoJobWithHttpInfo(any())).thenReturn(
                ResponseEntity.internalServerError().build());
        assertThrows(DmeException.class, () -> acDmeClient.deleteDataConsumer(Set.of("consumer1")));
    }

    private static Stream<Arguments> getDeleteDataProducers() {
        return Stream.of(Arguments.of(Set.of("producer1")), Arguments.of(Set.of()));
    }

    private static Stream<Arguments> getDeleteDataConsumers() {
        return Stream.of(Arguments.of(Set.of("consumer1")), Arguments.of(Set.of()));
    }

    private static Stream<Arguments> getInfoTypes() {
        return Stream.of(Arguments.of(Map.of("infotype1", "{}")), Arguments.of(Map.of()));
    }

    private static Stream<Arguments> getDataProducers() {
        return Stream.of(Arguments.of(Map.of("dataproducers", "{}")), Arguments.of(Map.of()));
    }

    private static Stream<Arguments> getDataConsumers() {
        return Stream.of(Arguments.of(Map.of("dataconsumer", "{}")), Arguments.of(Map.of()));
    }
}
