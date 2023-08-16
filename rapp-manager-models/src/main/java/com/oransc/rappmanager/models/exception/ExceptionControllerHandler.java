package com.oransc.rappmanager.models.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionControllerHandler {

    @ExceptionHandler(RappHandlerException.class)
    public ResponseEntity<ErrorResponse> handleRappHandlerException(RappHandlerException rappHandlerException) {
        return ResponseEntity.status(rappHandlerException.getStatusCode())
                       .body(new ErrorResponse(rappHandlerException.getMessage()));
    }
}
