package com.site.server.google;

import org.springframework.http.HttpStatus;

public class GoogleSignInException extends RuntimeException {
    private final HttpStatus status;
    private final String error;

    public GoogleSignInException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() { return status; }
    public String getError() { return error; }
}
