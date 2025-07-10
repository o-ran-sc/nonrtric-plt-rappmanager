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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import org.springframework.util.ReflectionUtils;

public class AtleastOneFieldShouldNotBeNullValidator
        implements ConstraintValidator<AtleastOneFieldShouldNotBeNull, Object> {

    private String[] fields;

    @Override
    public void initialize(AtleastOneFieldShouldNotBeNull constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext constraintValidatorContext) {
        if (obj == null) {
            return false;
        }
        try {
            for (String field : fields) {
                Field declaredField = obj.getClass().getDeclaredField(field);
                ReflectionUtils.makeAccessible(declaredField);
                if (declaredField.get(obj) != null) {
                    return true;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
        return false;
    }
}
