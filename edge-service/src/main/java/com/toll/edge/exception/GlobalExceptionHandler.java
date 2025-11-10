package com.toll.edge.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ManualInterventionRequiredException.class)
    public Map<String, Object> handleManual(ManualInterventionRequiredException ex) {

        return Map.of(
                "timestamp", Instant.now().toString(),
                "tagId", ex.getTagId(),
                "status", "MANUAL_REQUIRED",
                "reason", ex.getReason(),
                "message", ex.getActionMessage()
        );
    }
}
