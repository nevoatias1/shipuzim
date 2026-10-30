package com.site.server.contact;

import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactRequestController {
    private final ContactRequestService service;
    private final Validator validator;

    public ContactRequestController(ContactRequestService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    @PostMapping
    public ResponseEntity<ContactRequestResponse> create(@RequestBody ContactRequestDto request) {
        if (request.website() != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ContactRequestResponse(0L, LocalDateTime.now(ZoneOffset.UTC)));
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }
        ContactRequest saved = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactRequestResponse.from(saved));
    }
}
