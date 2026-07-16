package com.pixalportal.backend.exceptions;

import java.time.LocalDateTime;

public class ApiError {
    
    private final int status;
    private final String error;
    private final String message;
    private final LocalDateTime timestamp;

    public ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getters so Spring can convert this object into JSON
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}