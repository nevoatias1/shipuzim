package com.site.server.contact;

import java.time.LocalDateTime;

public record AdminContactRequestResponse(Long id, String fullName, String phone, String email, String city,
                                          WorkType workType, String description, RequestStatus status,
                                          LocalDateTime createdAt) {
    public static AdminContactRequestResponse from(ContactRequest request) {
        return new AdminContactRequestResponse(request.getId(), request.getFullName(), request.getPhone(),
                request.getEmail(), request.getCity(), request.getWorkType(), request.getDescription(),
                request.getStatus(), request.getCreatedAt());
    }

    @Override
    public String toString() { return "AdminContactRequestResponse[redacted]"; }
}
