/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2022 AT&T Intellectual Property. All rights reserved.
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

package org.oransc.participant.dme.utils;

import java.util.List;
import java.util.UUID;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class CommonTestData {

    private static final String TEST_KEY_NAME = "onap.policy.clamp.ac.element.DmeAutomationCompositionElement";
    private static final List<UUID> AC_ID_LIST = List.of(UUID.randomUUID(), UUID.randomUUID());

    public AcElementDeploy getAutomationCompositionElement() {
        var element = new AcElementDeploy();
        element.setId(UUID.randomUUID());
        element.setDefinition(new ToscaConceptIdentifier(TEST_KEY_NAME, "1.0.1"));
        element.setOrderedState(DeployOrder.DEPLOY);
        return element;
    }

    public ToscaConceptIdentifier getDmeIdentifier(int instanceNo) {
        return new ToscaConceptIdentifier("DmeInstance" + instanceNo, "1.0.0");
    }

    public UUID getAutomationCompositionId() {
        return getAutomationCompositionId(0);
    }

    public UUID getAutomationCompositionId(int instanceNo) {
        return AC_ID_LIST.get(instanceNo);
    }

}
