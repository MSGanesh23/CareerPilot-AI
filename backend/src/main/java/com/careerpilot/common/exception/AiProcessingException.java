package com.careerpilot.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AiProcessingException extends RuntimeException {
    public AiProcessingException(String message) {
        super(message);
    }
    public AiProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
