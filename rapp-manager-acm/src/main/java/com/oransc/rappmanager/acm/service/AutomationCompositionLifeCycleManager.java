/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

package com.oransc.rappmanager.acm.service;


import com.oransc.rappmanager.acm.configuration.ACMConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutomationCompositionLifeCycleManager implements SmartLifecycle {

    Logger logger = LoggerFactory.getLogger(AutomationCompositionLifeCycleManager.class);
    private final ACMConfiguration acmConfiguration;
    private final AcmDeployer acmDeployer;
    private boolean running;

    @Override
    public void start() {
        logger.info("Initializing automation Composition");
        try {
            String compositionPayload = Files.readString(Path.of(acmConfiguration.getCompositionDefinitionLocation()));
            CommissioningResponse commissioningResponse =
                    acmDeployer.createComposition(compositionPayload);
            if (commissioningResponse != null && commissioningResponse.getCompositionId() != null) {
                logger.info("Priming automation Composition");
                acmDeployer.primeACMComposition(commissioningResponse.getCompositionId(),
                        PrimeOrder.PRIME);
                for (int i = 0; i < acmConfiguration.getMaxRetries(); i++) {
                    logger.debug("Composition priming check {}", i + 1);
                    if (acmDeployer.isCompositionStateEquals(commissioningResponse.getCompositionId(),
                            AcTypeState.PRIMED)) {
                        logger.info("Composition {} is primed", commissioningResponse.getCompositionId());
                        running = true;
                        break;
                    } else {
                        TimeUnit.SECONDS.sleep(acmConfiguration.getRetryInterval());
                    }
                }
            } else {
                logger.error("Failed to create automation composition");
            }
        } catch (Exception e) {
            logger.error("Failed to create automation composition", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Depriming automation Composition");
        if (running) {
            try {
                acmDeployer.primeACMComposition(acmDeployer.getCompositionId(),
                        PrimeOrder.DEPRIME);
                for (int i = 0; i < acmConfiguration.getMaxRetries(); i++) {
                    logger.debug("Composition depriming check {}", i + 1);
                    if (acmDeployer.isCompositionStateEquals(
                            acmDeployer.getCompositionId(), AcTypeState.COMMISSIONED)) {
                        logger.info("Composition {} is deprimed", acmDeployer.getCompositionId());
                        logger.info("Deleting automation Composition");
                        acmDeployer.deleteComposition(acmDeployer.getCompositionId());
                        running = false;
                        break;
                    } else {
                        TimeUnit.SECONDS.sleep(acmConfiguration.getRetryInterval());
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to cleanup automation composition");
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
