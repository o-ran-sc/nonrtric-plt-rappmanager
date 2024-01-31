/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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
 */

package com.oransc.rappmanager.sme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.models.cache.RappCacheService;
import com.oransc.rappmanager.models.csar.RappCsarConfigurationHandler;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import com.oransc.rappmanager.models.rappinstance.RappSMEInstance;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachine;
import com.oransc.rappmanager.models.statemachine.RappInstanceStateMachineConfig;
import com.oransc.rappmanager.sme.configuration.SmeConfiguration;
import com.oransc.rappmanager.sme.invoker.data.APIInvokerEnrolmentDetails;
import com.oransc.rappmanager.sme.provider.data.APIProviderEnrolmentDetails;
import com.oransc.rappmanager.sme.provider.data.APIProviderFunctionDetails;
import com.oransc.rappmanager.sme.provider.data.ApiProviderFuncRole;
import com.oransc.rappmanager.sme.publishservice.data.AefProfile;
import com.oransc.rappmanager.sme.publishservice.data.ServiceAPIDescription;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(classes = {SmeConfiguration.class, SmeDeployer.class, BeanTestConfiguration.class,
        RappCsarConfigurationHandler.class, RappCacheService.class, RappInstanceStateMachineConfig.class,
        RappInstanceStateMachine.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
class SmeDeployerTest {

    MockRestServiceServer mockServer;
    @Autowired
    @SpyBean
    SmeDeployer smeDeployer;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    SmeConfiguration smeConfiguration;
    String validCsarFileLocation = "src/test/resources/";
    @Autowired
    RappInstanceStateMachine rappInstanceStateMachine;
    @Autowired
    ObjectMapper objectMapper;
    private final String validRappFile = "valid-rapp-package.csar";
    String URI_PROVIDER_REGISTRATIONS, URI_PROVIDER_REGISTRATION, URI_PUBLISH_APIS, URI_PUBLISH_API, URI_INVOKERS,
            URI_INVOKER;

    @BeforeAll
    void initSmeUri() {
        URI_PROVIDER_REGISTRATIONS =
                smeConfiguration.getBaseUrl() + smeConfiguration.getProviderBasePath() + "registrations";
        URI_PROVIDER_REGISTRATION =
                smeConfiguration.getBaseUrl() + smeConfiguration.getProviderBasePath() + "registrations/%s";
        URI_PUBLISH_APIS = smeConfiguration.getBaseUrl() + smeConfiguration.getPublishApiBasePath() + "%s/service-apis";
        URI_PUBLISH_API =
                smeConfiguration.getBaseUrl() + smeConfiguration.getPublishApiBasePath() + "%s/service-apis/%s";
        URI_INVOKERS = smeConfiguration.getBaseUrl() + smeConfiguration.getInvokerBasePath() + "onboardedInvokers";
        URI_INVOKER = smeConfiguration.getBaseUrl() + smeConfiguration.getInvokerBasePath() + "onboardedInvokers/%s";
    }

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testCreateAMF() throws JsonProcessingException {
        String apiProvDomId = UUID.randomUUID().toString();
        APIProviderEnrolmentDetails apiProviderEnrolmentDetails = new APIProviderEnrolmentDetails(apiProvDomId);
        mockServer.expect(ExpectedCount.once(), requestTo(URI_PROVIDER_REGISTRATIONS))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(apiProviderEnrolmentDetails)));
        APIProviderEnrolmentDetails apiProviderEnrolmentResponse = smeDeployer.createAMF();
        mockServer.verify();
        assertEquals(apiProvDomId, apiProviderEnrolmentResponse.getApiProvDomId());
    }

    @Test
    void testCreateAMFFailure() {
        mockServer.expect(ExpectedCount.once(), requestTo(URI_PROVIDER_REGISTRATIONS))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        APIProviderEnrolmentDetails apiProviderEnrolmentResponse = smeDeployer.createAMF();
        mockServer.verify();
        assertNull(apiProviderEnrolmentResponse);
    }

    @Test
    void testDeleteAMF() throws JsonProcessingException {
        String apiProvDomId = UUID.randomUUID().toString();
        APIProviderEnrolmentDetails apiProviderEnrolmentDetails = new APIProviderEnrolmentDetails(apiProvDomId);
        mockServer.expect(ExpectedCount.once(), requestTo(URI_PROVIDER_REGISTRATIONS))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(apiProviderEnrolmentDetails)));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PROVIDER_REGISTRATION, apiProvDomId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        smeDeployer.createAMF();
        smeDeployer.deleteAMF();
        mockServer.verify();
    }

    @Test
    void testCreateProviderDomain() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        APIProviderEnrolmentDetails apiProviderEnrolmentDetails = getProviderDomainApiEnrollmentDetails();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_PROVIDER_REGISTRATIONS))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(apiProviderEnrolmentDetails)));
        RappInstance rappInstance = getRappInstance();
        boolean createProviderDomain = smeDeployer.createProviderDomain(rapp, rappInstance);
        mockServer.verify();
        assertTrue(createProviderDomain);
    }

    @Test
    void testCreateProviderDomainFailure() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_PROVIDER_REGISTRATIONS))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        RappInstance rappInstance = getRappInstance();
        boolean createProviderDomain = smeDeployer.createProviderDomain(rapp, rappInstance);
        mockServer.verify();
        assertFalse(createProviderDomain);
    }

    @Test
    void testDeleteProviderFunc() {
        UUID registrationId = UUID.randomUUID();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PROVIDER_REGISTRATION, registrationId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        smeDeployer.deleteProviderFunc(String.valueOf(registrationId));
        mockServer.verify();
    }

    @Test
    void testCreatePublishApi() throws Exception {
        UUID rappId = UUID.randomUUID();
        UUID apfId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        ServiceAPIDescription serviceAPIDescription = getServiceApiDescription();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_APIS, apfId)))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(serviceAPIDescription)));
        RappInstance rappInstance = getRappInstance();
        rappInstance.getSme().setApfId(String.valueOf(apfId));
        boolean publishApi = smeDeployer.createPublishApi(rapp, rappInstance);
        mockServer.verify();
        assertTrue(publishApi);
    }


    @Test
    void testCreatePublishApiFailure() {
        UUID rappId = UUID.randomUUID();
        UUID apfId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_APIS, apfId)))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        RappInstance rappInstance = getRappInstance();
        rappInstance.getSme().setApfId(String.valueOf(apfId));
        boolean publishApi = smeDeployer.createPublishApi(rapp, rappInstance);
        mockServer.verify();
        assertFalse(publishApi);
    }

    @Test
    void testDeletePublishApi() {
        String serviceApiId = String.valueOf(UUID.randomUUID());
        String apfId = String.valueOf(UUID.randomUUID());
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_API, apfId, serviceApiId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        smeDeployer.deletePublishApi(serviceApiId, apfId);
        mockServer.verify();
    }

    @Test
    void testCreateInvoker() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        APIInvokerEnrolmentDetails apiInvokerEnrolmentDetails = getApiInvokerEnrollmentDetails();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_INVOKERS)).andExpect(method(HttpMethod.POST)).andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(apiInvokerEnrolmentDetails)));
        RappInstance rappInstance = getRappInstance();
        boolean createInvoker = smeDeployer.createInvoker(rapp, rappInstance);
        mockServer.verify();
        assertTrue(createInvoker);
    }

    @Test
    void testCreateInvokerFailure() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_INVOKERS)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        RappInstance rappInstance = getRappInstance();
        boolean createInvoker = smeDeployer.createInvoker(rapp, rappInstance);
        mockServer.verify();
        assertFalse(createInvoker);
    }

    @Test
    void testDeleteInvoker() {
        String invokerId = String.valueOf(UUID.randomUUID());
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_INVOKER, invokerId)))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        smeDeployer.deleteInvoker(invokerId);
        mockServer.verify();
    }

    @Test
    void testDeployRappInstance() throws Exception {
        UUID rappId = UUID.randomUUID();
        APIProviderEnrolmentDetails apiProviderEnrolmentDetails = getProviderDomainApiEnrollmentDetails();
        APIProviderFunctionDetails apfProviderFunctionDetails = apiProviderEnrolmentDetails.getApiProvFuncs().stream()
                                                                        .filter(apiProviderFunctionDetails -> apiProviderFunctionDetails.getApiProvFuncRole()
                                                                                                                      .equals(ApiProviderFuncRole.APF))
                                                                        .findFirst().get();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_PROVIDER_REGISTRATIONS))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(apiProviderEnrolmentDetails)));
        ServiceAPIDescription serviceAPIDescription = getServiceApiDescription();
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PUBLISH_APIS, apfProviderFunctionDetails.getApiProvFuncId())))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(serviceAPIDescription)));
        APIInvokerEnrolmentDetails apiInvokerEnrolmentDetails = getApiInvokerEnrollmentDetails();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_INVOKERS)).andExpect(method(HttpMethod.POST)).andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(apiInvokerEnrolmentDetails)));
        RappInstance rappInstance = getRappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean deployRapp = smeDeployer.deployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertTrue(deployRapp);
    }

    @Test
    void testDeployRappInstanceNoInvoker() throws Exception {
        UUID rappId = UUID.randomUUID();
        APIProviderEnrolmentDetails apiProviderEnrolmentDetails = getProviderDomainApiEnrollmentDetails();
        APIProviderFunctionDetails apfProviderFunctionDetails = apiProviderEnrolmentDetails.getApiProvFuncs().stream()
                                                                        .filter(apiProviderFunctionDetails -> apiProviderFunctionDetails.getApiProvFuncRole()
                                                                                                                      .equals(ApiProviderFuncRole.APF))
                                                                        .findFirst().get();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_PROVIDER_REGISTRATIONS))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(apiProviderEnrolmentDetails)));
        ServiceAPIDescription serviceAPIDescription = getServiceApiDescription();
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PUBLISH_APIS, apfProviderFunctionDetails.getApiProvFuncId())))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(serviceAPIDescription)));
        RappInstance rappInstance = getRappInstance();
        rappInstance.getSme().setInvokers(null);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean deployRapp = smeDeployer.deployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertTrue(deployRapp);
    }

    @Test
    void testDeployRappInstanceNoProvider() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        APIInvokerEnrolmentDetails apiInvokerEnrolmentDetails = getApiInvokerEnrollmentDetails();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_INVOKERS)).andExpect(method(HttpMethod.POST)).andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(apiInvokerEnrolmentDetails)));
        RappInstance rappInstance = getRappInstance();
        rappInstance.getSme().setProviderFunction(null);
        rappInstance.getSme().setServiceApis(null);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean deployRapp = smeDeployer.deployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertTrue(deployRapp);
    }

    @Test
    void testDeployRappInstanceWithoutSme() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.PRIMED).build();
        RappInstance rappInstance = getRappInstance();
        rappInstance.setSme(null);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        assertTrue(smeDeployer.deployRappInstance(rapp, rappInstance));
    }

    @Test
    void testDeployRappFailure() throws Exception {
        UUID rappId = UUID.randomUUID();
        APIProviderEnrolmentDetails apiProviderEnrolmentDetails = getProviderDomainApiEnrollmentDetails();
        APIProviderFunctionDetails apfProviderFunctionDetails = apiProviderEnrolmentDetails.getApiProvFuncs().stream()
                                                                        .filter(apiProviderFunctionDetails -> apiProviderFunctionDetails.getApiProvFuncRole()
                                                                                                                      .equals(ApiProviderFuncRole.APF))
                                                                        .findFirst().get();
        Rapp rapp =
                Rapp.builder().rappId(rappId).name("").packageName(validRappFile).packageLocation(validCsarFileLocation)
                        .state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(URI_PROVIDER_REGISTRATIONS))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(apiProviderEnrolmentDetails)));
        ServiceAPIDescription serviceAPIDescription = getServiceApiDescription();
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PUBLISH_APIS, apfProviderFunctionDetails.getApiProvFuncId())))
                .andExpect(method(HttpMethod.POST)).andRespond(
                        withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                                .body(objectMapper.writeValueAsString(serviceAPIDescription)));
        mockServer.expect(ExpectedCount.once(), requestTo(URI_INVOKERS)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        RappInstance rappInstance = getRappInstance();
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean deployRapp = smeDeployer.deployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertFalse(deployRapp);
    }

    @Test
    void testUndeployRappInstance() {
        UUID rappId = UUID.randomUUID();
        UUID apfId = UUID.randomUUID();
        List<String> invokers = List.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        List<String> serviceApis = List.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        Map<String, String> providerFuncs = Map.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()),
                String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        Rapp rapp = Rapp.builder().rappId(rappId).name(rappId.toString()).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_INVOKER, invokers.get(0))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_INVOKER, invokers.get(1))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_API, apfId, serviceApis.get(0))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_API, apfId, serviceApis.get(1))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PROVIDER_REGISTRATION, providerFuncs.values().toArray()[0])))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PROVIDER_REGISTRATION, providerFuncs.values().toArray()[1])))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        RappInstance rappInstance = getRappInstance();
        rappInstance.getSme().setApfId(String.valueOf(apfId));
        rappInstance.getSme().setProviderFunctionIds(providerFuncs.values().stream().toList());
        rappInstance.getSme().setServiceApiIds(serviceApis);
        rappInstance.getSme().setInvokerIds(invokers);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean undeployRapp = smeDeployer.undeployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertTrue(undeployRapp);
    }

    @Test
    void testUndeployRappInstanceNoInvokers() {
        UUID rappId = UUID.randomUUID();
        UUID apfId = UUID.randomUUID();
        List<String> serviceApis = List.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        Map<String, String> providerFuncs = Map.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()),
                String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        Rapp rapp = Rapp.builder().rappId(rappId).name(rappId.toString()).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_API, apfId, serviceApis.get(0))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_API, apfId, serviceApis.get(1))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PROVIDER_REGISTRATION, providerFuncs.values().toArray()[0])))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PROVIDER_REGISTRATION, providerFuncs.values().toArray()[1])))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        RappInstance rappInstance = getRappInstance();
        rappInstance.getSme().setApfId(String.valueOf(apfId));
        rappInstance.getSme().setProviderFunctionIds(providerFuncs.values().stream().toList());
        rappInstance.getSme().setServiceApiIds(serviceApis);
        rappInstance.getSme().setInvokerIds(null);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean undeployRapp = smeDeployer.undeployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertTrue(undeployRapp);
    }

    @Test
    void testUndeployRappInstanceNoProviders() {
        UUID rappId = UUID.randomUUID();
        UUID apfId = UUID.randomUUID();
        List<String> invokers = List.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        Rapp rapp = Rapp.builder().rappId(rappId).name(rappId.toString()).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_INVOKER, invokers.get(0))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_INVOKER, invokers.get(1))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        RappInstance rappInstance = getRappInstance();
        rappInstance.getSme().setApfId(String.valueOf(apfId));
        rappInstance.getSme().setProviderFunctionIds(null);
        rappInstance.getSme().setServiceApiIds(null);
        rappInstance.getSme().setInvokerIds(invokers);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean undeployRapp = smeDeployer.undeployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertTrue(undeployRapp);
    }

    @Test
    void testUndeployRappInstanceWithoutSme() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(rappId.toString()).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        RappInstance rappInstance = getRappInstance();
        rappInstance.setSme(null);
        assertTrue(smeDeployer.undeployRappInstance(rapp, rappInstance));
    }

    @Test
    void testUndeployRappInstanceFailure() {
        UUID rappId = UUID.randomUUID();
        UUID apfId = UUID.randomUUID();
        List<String> invokers = List.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        List<String> serviceApis = List.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        Map<String, String> providerFuncs = Map.of(String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()),
                String.valueOf(UUID.randomUUID()), String.valueOf(UUID.randomUUID()));
        Rapp rapp = Rapp.builder().rappId(rappId).name(rappId.toString()).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_INVOKER, invokers.get(0))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_INVOKER, invokers.get(1))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_API, apfId, serviceApis.get(0))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(), requestTo(String.format(URI_PUBLISH_API, apfId, serviceApis.get(1))))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PROVIDER_REGISTRATION, providerFuncs.values().toArray()[0])))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(ExpectedCount.once(),
                        requestTo(String.format(URI_PROVIDER_REGISTRATION, providerFuncs.values().toArray()[1])))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        RappInstance rappInstance = getRappInstance();
        rappInstance.getSme().setApfId(String.valueOf(apfId));
        rappInstance.getSme().setProviderFunctionIds(providerFuncs.values().stream().toList());
        rappInstance.getSme().setServiceApiIds(serviceApis);
        rappInstance.getSme().setInvokerIds(invokers);
        rappInstanceStateMachine.onboardRappInstance(rappInstance.getRappInstanceId());
        boolean undeployRapp = smeDeployer.undeployRappInstance(rapp, rappInstance);
        mockServer.verify();
        assertFalse(undeployRapp);
    }

    @Test
    void testPrimingAndDeprimingDoesNothing() {
        assertTrue(smeDeployer.primeRapp(null));
        assertTrue(smeDeployer.deprimeRapp(null));
    }

    RappInstance getRappInstance() {
        RappSMEInstance rappSMEInstance = new RappSMEInstance();
        rappSMEInstance.setInvokers("invoker-app1");
        rappSMEInstance.setServiceApis("api-set-1");
        rappSMEInstance.setAefId("TEST_AEF_ID");
        rappSMEInstance.setApfId(String.valueOf(UUID.randomUUID()));
        rappSMEInstance.setProviderFunction("aef-provider-function");
        RappInstance rappInstance = new RappInstance();
        rappInstance.setSme(rappSMEInstance);
        return rappInstance;
    }

    APIProviderEnrolmentDetails getProviderDomainApiEnrollmentDetails() {
        APIProviderEnrolmentDetails apiProviderEnrolmentDetails =
                new APIProviderEnrolmentDetails(UUID.randomUUID().toString());
        APIProviderFunctionDetails apiProviderFunctionDetailsAEF = new APIProviderFunctionDetails();
        apiProviderFunctionDetailsAEF.setApiProvFuncInfo("AEF");
        apiProviderFunctionDetailsAEF.setApiProvFuncRole(ApiProviderFuncRole.AEF);
        apiProviderFunctionDetailsAEF.setApiProvFuncId(String.valueOf(UUID.randomUUID()));
        APIProviderFunctionDetails apiProviderFunctionDetailsAPF = new APIProviderFunctionDetails();
        apiProviderFunctionDetailsAPF.setApiProvFuncInfo("APF");
        apiProviderFunctionDetailsAPF.setApiProvFuncRole(ApiProviderFuncRole.APF);
        apiProviderFunctionDetailsAPF.setApiProvFuncId(String.valueOf(UUID.randomUUID()));
        apiProviderEnrolmentDetails.setApiProvFuncs(
                List.of(apiProviderFunctionDetailsAEF, apiProviderFunctionDetailsAPF));
        return apiProviderEnrolmentDetails;
    }


    ServiceAPIDescription getServiceApiDescription() {
        ServiceAPIDescription serviceAPIDescription = new ServiceAPIDescription();
        serviceAPIDescription.setApiId(String.valueOf(UUID.randomUUID()));
        AefProfile aefProfile = new AefProfile();
        aefProfile.setAefId(String.valueOf(UUID.randomUUID()));
        serviceAPIDescription.setAefProfiles(List.of(aefProfile));
        return serviceAPIDescription;
    }

    APIInvokerEnrolmentDetails getApiInvokerEnrollmentDetails() {
        APIInvokerEnrolmentDetails apiInvokerEnrolmentDetails =
                new APIInvokerEnrolmentDetails(String.valueOf(UUID.randomUUID()));
        com.oransc.rappmanager.sme.invoker.data.ServiceAPIDescription serviceAPIDescription =
                new com.oransc.rappmanager.sme.invoker.data.ServiceAPIDescription();
        serviceAPIDescription.setApiId(String.valueOf(UUID.randomUUID()));
        apiInvokerEnrolmentDetails.setApiList(List.of(serviceAPIDescription));
        return apiInvokerEnrolmentDetails;
    }
}
