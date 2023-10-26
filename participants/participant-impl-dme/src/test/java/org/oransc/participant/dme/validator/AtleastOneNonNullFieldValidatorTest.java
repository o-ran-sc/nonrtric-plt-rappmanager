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

package org.oransc.participant.dme.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.oransc.participant.dme.models.ConfigurationEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AtleastOneNonNullFieldValidatorTest {

    @Mock
    AtleastOneNonNullField atleastOneNonNullField;
    @Mock
    ConstraintValidatorContext constraintValidatorContext;

    String[] fields = new String[] {"infoTypeEntities", "dataProducerEntities", "dataConsumerEntities"};

    @Test
    void testValidObject() {
        when(atleastOneNonNullField.fieldNames()).thenReturn(fields);
        AtleastOneNonNullFieldValidator atleastOneNonNullFieldValidator = new AtleastOneNonNullFieldValidator();
        atleastOneNonNullFieldValidator.initialize(atleastOneNonNullField);
        ConfigurationEntity configurationEntity = new ConfigurationEntity(List.of(), List.of(), List.of());
        assertTrue(atleastOneNonNullFieldValidator.isValid(configurationEntity, constraintValidatorContext));
    }

    @Test
    void testInvalidObject() {
        when(atleastOneNonNullField.fieldNames()).thenReturn(fields);
        AtleastOneNonNullFieldValidator atleastOneNonNullFieldValidator = new AtleastOneNonNullFieldValidator();
        atleastOneNonNullFieldValidator.initialize(atleastOneNonNullField);
        ConfigurationEntity configurationEntity = new ConfigurationEntity(null, null, null);
        assertFalse(atleastOneNonNullFieldValidator.isValid(configurationEntity, constraintValidatorContext));
    }

    @Test
    void testInvalidField() {
        when(atleastOneNonNullField.fieldNames()).thenReturn(new String[] {"invalidField"});
        AtleastOneNonNullFieldValidator atleastOneNonNullFieldValidator = new AtleastOneNonNullFieldValidator();
        atleastOneNonNullFieldValidator.initialize(atleastOneNonNullField);
        ConfigurationEntity configurationEntity = new ConfigurationEntity(List.of(), List.of(), List.of());
        assertFalse(atleastOneNonNullFieldValidator.isValid(configurationEntity, constraintValidatorContext));
    }

    @Test
    void testNullObject() {
        when(atleastOneNonNullField.fieldNames()).thenReturn(new String[] {"invalidField"});
        AtleastOneNonNullFieldValidator atleastOneNonNullFieldValidator = new AtleastOneNonNullFieldValidator();
        atleastOneNonNullFieldValidator.initialize(atleastOneNonNullField);
        assertFalse(atleastOneNonNullFieldValidator.isValid(null, constraintValidatorContext));
    }
}
