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

package org.oransc.participant.dme.exception;

import jakarta.ws.rs.core.Response;
import java.io.Serial;
import org.onap.policy.models.base.PfModelException;

public class DmeException extends PfModelException {

    @Serial
    private static final long serialVersionUID = 6438746904567105054L;

    public DmeException(int statusCode, String message) {
        super(Response.Status.fromStatusCode(statusCode), message);
    }

    public DmeException(int statusCode, String message, Exception e) {
        super(Response.Status.fromStatusCode(statusCode), message, e);
    }
}
