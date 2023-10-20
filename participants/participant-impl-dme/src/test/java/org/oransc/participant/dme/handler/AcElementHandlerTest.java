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

package org.oransc.participant.dme.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.oransc.participant.dme.exception.DmeException;
import org.oransc.participant.dme.restclient.AcDmeClient;
import org.oransc.participant.dme.utils.CommonTestData;
import org.oransc.participant.dme.utils.ToscaUtils;

class AcElementHandlerTest {

    private final AcDmeClient acDmeClient = mock(AcDmeClient.class);

    private final CommonTestData commonTestData = new CommonTestData();

    private static ToscaServiceTemplate serviceTemplate;
    private static final String DME_AUTOMATION_COMPOSITION_ELEMENT =
            "onap.policy.clamp.ac.element.DMEAutomationCompositionElement";

    @BeforeAll
    static void init() {
        serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
    }

    @BeforeEach
    void startMocks() throws DmeException, JsonProcessingException {
        when(acDmeClient.isDmeHealthy()).thenReturn(Boolean.TRUE);
        doNothing().when(acDmeClient).createInfoType(any());
    }

    @Test
    void test_automationCompositionElementStateChange() throws DmeException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var automationCompositionId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var automationCompositionElementId = element.getId();

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(DME_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                automationCompositionElementId, DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");

        automationCompositionElementHandler.undeploy(automationCompositionId, automationCompositionElementId);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Undeployed");

        when(acDmeClient.isDmeHealthy()).thenReturn(Boolean.FALSE);
        assertThrows(DmeException.class, () -> automationCompositionElementHandler
                .undeploy(automationCompositionId, automationCompositionElementId));
    }

    @Test
    void test_AutomationCompositionElementUpdate() throws DmeException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var element = commonTestData.getAutomationCompositionElement();
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(DME_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(
                commonTestData.getAutomationCompositionId(), element.getId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Deployed");
    }

    @Test
    void test_AutomationCompositionElementUpdateWithUnhealthyA1pms() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var element = commonTestData.getAutomationCompositionElement();
        when(acDmeClient.isDmeHealthy()).thenReturn(Boolean.FALSE);

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        assertThrows(DmeException.class,
                () -> automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                        nodeTemplatesMap.get(DME_AUTOMATION_COMPOSITION_ELEMENT).getProperties()));
    }

    @Test
    void test_AutomationCompositionElementUpdateWithInvalidConfiguration() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var element = commonTestData.getAutomationCompositionElement();
        assertThrows(DmeException.class, () -> automationCompositionElementHandler
                .deploy(commonTestData.getAutomationCompositionId(), element, Map.of()));
    }

    @Test
    void testLock() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var automationCompositionId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        automationCompositionElementHandler.lock(automationCompositionId, elementId);

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId, elementId,
                null, LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
    }

    @Test
    void testUnlock() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var automationCompositionId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        automationCompositionElementHandler.unlock(automationCompositionId, elementId);

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId, elementId,
                null, LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Test
    void testUpdate() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var automationCompositionId = UUID.randomUUID();
        var element = commonTestData.getAutomationCompositionElement();
        automationCompositionElementHandler.update(automationCompositionId, element, Map.of());

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                element.getId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Test
    void testDelete() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var automationCompositionId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        automationCompositionElementHandler.delete(automationCompositionId, elementId);

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId, elementId,
                DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
    }

    @Test
    void testPrime() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var compositionId = UUID.randomUUID();
        automationCompositionElementHandler.prime(compositionId, List.of());

        verify(participantIntermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "Primed");
    }

    @Test
    void testDeprime() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var compositionId = UUID.randomUUID();
        automationCompositionElementHandler.deprime(compositionId);

        verify(participantIntermediaryApi).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed");
    }

    @Test
    void testHandleRestartComposition() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var compositionId = UUID.randomUUID();
        automationCompositionElementHandler.handleRestartComposition(compositionId, List.of(), AcTypeState.PRIMED);

        verify(participantIntermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "Restarted");
    }

    @Test
    void testHandleRestartInstanceDeploying() throws PfModelException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var automationCompositionId = UUID.randomUUID();
        var element = commonTestData.getAutomationCompositionElement();
        var automationCompositionElementId = element.getId();
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.handleRestartInstance(automationCompositionId, element,
                nodeTemplatesMap.get(DME_AUTOMATION_COMPOSITION_ELEMENT).getProperties(), DeployState.DEPLOYING,
                LockState.NONE);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                automationCompositionElementId, DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
    }

    @Test
    void testHandleRestartInstanceDeployed() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(intermediaryApi, acDmeClient);

        var automationCompositionId = UUID.randomUUID();
        var element = commonTestData.getAutomationCompositionElement();
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.handleRestartInstance(automationCompositionId, element,
                nodeTemplatesMap.get(DME_AUTOMATION_COMPOSITION_ELEMENT).getProperties(), DeployState.DEPLOYED,
                LockState.LOCKED);
        verify(intermediaryApi).updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                DeployState.DEPLOYED, LockState.LOCKED, StateChangeResult.NO_ERROR, "Restarted");
    }

    @Test
    void testHandleRestartInstanceUndeployed() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(intermediaryApi, acDmeClient);

        var automationCompositionId = UUID.randomUUID();
        var element = commonTestData.getAutomationCompositionElement();
        var automationCompositionElementId = element.getId();
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.handleRestartInstance(automationCompositionId, element,
                nodeTemplatesMap.get(DME_AUTOMATION_COMPOSITION_ELEMENT).getProperties(), DeployState.UNDEPLOYING,
                LockState.LOCKED);
        verify(intermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Undeployed");
    }

    @Test
    void testMigrate() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acDmeClient);

        var automationCompositionId = UUID.randomUUID();
        var element = commonTestData.getAutomationCompositionElement();
        automationCompositionElementHandler.migrate(automationCompositionId, element, UUID.randomUUID(), Map.of());

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                element.getId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
