/*
 * ============LICENSE_START======================================================================
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

package org.oransc.rappmanager.rest;

import java.util.Objects;
import java.util.stream.Collectors;
import org.oransc.rappmanager.models.exception.ErrorResponse;
import org.oransc.rappmanager.models.exception.RappHandlerException;
import org.oransc.rappmanager.models.exception.RappValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ExceptionControllerHandler {

    @ExceptionHandler(RappHandlerException.class)
    public ResponseEntity<ErrorResponse> handleRappHandlerException(RappHandlerException rappHandlerException) {
        return ResponseEntity.status(rappHandlerException.getStatusCode())
                       .body(new ErrorResponse(rappHandlerException.getMessage()));
    }

    @ExceptionHandler(RappValidationException.class)
    public ResponseEntity<ErrorResponse> handleRappValidationException(
            RappValidationException rappValidationException) {
        return ResponseEntity.status(rappValidationException.getStatusCode())
                       .body(new ErrorResponse(rappValidationException.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                       .body(new ErrorResponse("Request body is not in the expected format."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = "Validation failed for the request body: [ %s ]";
        if (ex.getBindingResult().getFieldErrors().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                    "Validation failed for the request body. " + Objects.requireNonNull(ex.getGlobalError())
                                                                         .getDefaultMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(String.format(errorMessage,
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                        .collect(Collectors.joining(", ")))));
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> handleMultipartException(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                       .body(new ErrorResponse("Request multipart is not valid or not in the expected format."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String errorMessage = "Parameter '%s' is not in the expected format.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                       .body(new ErrorResponse(String.format(errorMessage, ex.getName())));
    }
}
