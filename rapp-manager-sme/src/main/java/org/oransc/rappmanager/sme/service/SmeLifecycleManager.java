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

package org.oransc.rappmanager.sme.service;

import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.sme.provider.data.APIProviderEnrolmentDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmeLifecycleManager implements SmartLifecycle {

    Logger logger = LoggerFactory.getLogger(SmeLifecycleManager.class);

    private final SmeDeployer smeDeployer;
    private boolean running;

    @Override
    public void start() {
        try {
            logger.info("Registering Rapp Manager as AMF");
            APIProviderEnrolmentDetails providerServiceAMF = smeDeployer.createAMF();
            logger.info("Rapp Manager AMF Registration Id: {}", providerServiceAMF.getApiProvDomId());
            running = true;
        } catch (Exception e) {
            logger.warn("Error in initializing AMF", e);
            running = false;
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            logger.info("Deleting Rapp Manager as AMF");
            smeDeployer.deleteAMF();
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
