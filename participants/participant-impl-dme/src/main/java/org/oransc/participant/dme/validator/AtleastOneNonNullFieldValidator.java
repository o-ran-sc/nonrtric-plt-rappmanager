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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import org.springframework.beans.BeanUtils;

public class AtleastOneNonNullFieldValidator implements ConstraintValidator<AtleastOneNonNullField, Object> {

    String[] fieldNames;

    @Override
    public void initialize(AtleastOneNonNullField atleastOneNonNullField) {
        fieldNames = atleastOneNonNullField.fieldNames();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        if (object == null) {
            return false;
        }
        for (String fieldName : fieldNames) {
            try {
                PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(object.getClass(), fieldName);
                if (propertyDescriptor != null) {
                    Object fieldValue = propertyDescriptor.getReadMethod().invoke(object);
                    if (fieldValue != null) {
                        return true;
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                return false;
            }
        }
        return false;
    }
}
