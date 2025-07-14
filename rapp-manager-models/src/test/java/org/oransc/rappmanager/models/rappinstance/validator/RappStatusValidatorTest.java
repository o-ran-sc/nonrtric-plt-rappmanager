/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
 *
 */

package org.oransc.rappmanager.models.rappinstance.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappState;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {RappStatusValidator.class})
class RappStatusValidatorTest {

    @Autowired
    private RappStatusValidator rappStatusValidator;

    @Test
    void testRappStatusValidatorSuccess() {
        Rapp rapp = Rapp.builder().state(RappState.PRIMED).build();
        RappInstance rAppInstance = new RappInstance();
        assertDoesNotThrow(() -> rappStatusValidator.validate(rapp, rAppInstance));
    }

    @Test
    void testRappStatusValidatorFailure() {
        Rapp rapp = Rapp.builder().state(RappState.COMMISSIONED).build();
        RappInstance rAppInstance = new RappInstance();
        RappValidationException rappValidationException =
                assertThrows(RappValidationException.class, () -> rappStatusValidator.validate(rapp, rAppInstance));
        assertEquals("Unable to create rApp instance as rApp is not in PRIMED state",
                rappValidationException.getMessage());
    }

}
