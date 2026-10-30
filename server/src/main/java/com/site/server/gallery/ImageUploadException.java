package com.site.server.gallery;

import org.springframework.http.HttpStatus;

public class ImageUploadException extends RuntimeException {
    private final HttpStatus status;
    private final String error;

    public ImageUploadException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() { return status; }
    public String getError() { return error; }
}
