package com.site.server;

import com.site.server.gallery.ImageUploadException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> missingResource(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "NOT_FOUND"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> unsupportedMethod(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("error", "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<Map<String, String>> imageUpload(ImageUploadException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("error", exception.getError(), "message", exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "IMAGE_TOO_LARGE", "message", "Images must be at most 10 MB; multipart requests at most 11 MB."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> malformedMultipart(MultipartException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_MULTIPART", "message", "Invalid multipart upload."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("error", HttpStatus.valueOf(exception.getStatusCode().value()).name()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> validationFailed(ConstraintViolationException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fields.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "VALIDATION_FAILED", "fields", fields));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validationFailed(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of("error", "VALIDATION_FAILED", "fields", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformedJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "MALFORMED_JSON"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpectedError(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_ERROR", "message", "An unexpected error occurred."));
    }
}
