package com.site.server.contact;

import jakarta.validation.Valid;
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

    public ContactRequestController(ContactRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ContactRequestResponse> create(@Valid @RequestBody ContactRequestDto request) {
        ContactRequest saved = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactRequestResponse.from(saved));
    }
}