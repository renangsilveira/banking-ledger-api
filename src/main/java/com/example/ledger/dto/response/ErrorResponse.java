package com.example.ledger.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp,
    Map<String, String> violations
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), null);
    }

    public static ErrorResponse withViolations(int status, String error, String message, String path, Map<String, String> violations) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), violations);
    }
}
