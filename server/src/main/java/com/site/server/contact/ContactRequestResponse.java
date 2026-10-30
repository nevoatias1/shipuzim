package com.site.server.contact;

import java.time.LocalDateTime;

public record ContactRequestResponse(Long id, LocalDateTime createdAt) {
    public static ContactRequestResponse from(ContactRequest request) {
        return new ContactRequestResponse(request.getId(), request.getCreatedAt());
    }
}