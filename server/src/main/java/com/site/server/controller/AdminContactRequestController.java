package com.site.server.controller;

import com.site.server.contact.AdminContactRequestService;
import com.site.server.contact.ContactRequest;
import com.site.server.contact.RequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/contact-requests")
public class AdminContactRequestController {
    private final AdminContactRequestService service;

    public AdminContactRequestController(AdminContactRequestService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ContactRequest> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable long id, @Valid @RequestBody StatusUpdate request) {
        service.updateStatus(id, request.status());
        return ResponseEntity.noContent().build();
    }

    public record StatusUpdate(@NotNull RequestStatus status) {
    }
}
