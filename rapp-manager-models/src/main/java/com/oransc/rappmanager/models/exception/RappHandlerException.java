package com.oransc.rappmanager.models.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class RappHandlerException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public RappHandlerException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;

    }

}
