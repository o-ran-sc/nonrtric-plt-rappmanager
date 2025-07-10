/*-
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
 */

package org.oransc.rappmanager.models.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Payload;
import org.junit.jupiter.api.Test;

class AtleastOneFieldShouldNotBeNullValidatorTest {

    private final AtleastOneFieldShouldNotBeNullValidator validator = new AtleastOneFieldShouldNotBeNullValidator();

    @Test
    void testValidationWithAllNullFields() {
        validator.initialize(getAnnotation());
        Object testObject = new Object() {
            public final String acm = null;
            public final String sme = null;
            public final String dme = null;
        };
        assertFalse(validator.isValid(testObject, null));
    }

    @Test
    void testValidationWithNonNullFields() {
        validator.initialize(getAnnotation());
        Object testObjectAcmNotNull = new Object() {
            public final String acm = "data";
            public final String sme = null;
            public final String dme = null;
        };
        Object testObjectSmeNotNull = new Object() {
            public final String acm = null;
            public final String sme = "data";
            public final String dme = null;
        };
        Object testObjectDmeNotNull = new Object() {
            public final String acm = null;
            public final String sme = null;
            public final String dme = "data";
        };
        assertTrue(validator.isValid(testObjectAcmNotNull, null));
        assertTrue(validator.isValid(testObjectSmeNotNull, null));
        assertTrue(validator.isValid(testObjectDmeNotNull, null));
    }

    @Test
    void testValidationWithNullObject() {
        validator.initialize(getAnnotation());
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void testValidationInvalidFieldConfiguration() {
        validator.initialize(getAnnotation());
        Object testObjectInvalidField = new Object() {
            public final String acm = null;
            public final String sme = null;
        };
        assertFalse(validator.isValid(testObjectInvalidField, null));
    }

    AtleastOneFieldShouldNotBeNull getAnnotation() {
        return new AtleastOneFieldShouldNotBeNull() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AtleastOneFieldShouldNotBeNull.class;
            }

            @Override
            public String[] fields() {
                return new String[] {"acm", "sme", "dme"};
            }

            @Override
            public String message() {
                return "At least one field must not be null";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }
        };
    }
}