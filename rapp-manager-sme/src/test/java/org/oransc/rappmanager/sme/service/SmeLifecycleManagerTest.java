/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.oransc.rappmanager.sme.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.sme.provider.data.APIProviderEnrolmentDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = SmeLifecycleManager.class)
class SmeLifecycleManagerTest {

    @Autowired
    @MockitoBean
    SmeDeployer smeDeployer;

    @Autowired
    @MockitoSpyBean
    SmeLifecycleManager smeLifecycleManager;

    @Test
    void testStartWithSuccess() {
        when(smeDeployer.createAMF()).thenReturn(new APIProviderEnrolmentDetails());
        smeLifecycleManager.start();
        assertTrue(smeLifecycleManager.isRunning());
    }

    @Test
    void testStartWithFailure() {
        when(smeDeployer.createAMF()).thenReturn(null);
        smeLifecycleManager.start();
        assertFalse(smeLifecycleManager.isRunning());
    }

    @Test
    void testStopWithSuccess() {
        doNothing().when(smeDeployer).deleteAMF();
        when(smeDeployer.createAMF()).thenReturn(new APIProviderEnrolmentDetails());
        smeLifecycleManager.start();
        assertTrue(smeLifecycleManager.isRunning());
        smeLifecycleManager.stop();
        verify(smeDeployer, times(1)).deleteAMF();
        assertFalse(smeLifecycleManager.isRunning());
    }

    @Test
    void testStopWithoutStart() {
        smeLifecycleManager.stop();
        verify(smeDeployer, times(0)).deleteAMF();
        assertFalse(smeLifecycleManager.isRunning());
    }
}
