package com.site.server.contact;

import java.util.List;
import org.springframework.data.domain.Page;

public record AdminContactRequestPage(List<AdminContactRequestResponse> items, int page, int size,
                                     long totalElements, int totalPages) {
    public static AdminContactRequestPage from(Page<ContactRequest> result) {
        return new AdminContactRequestPage(result.getContent().stream().map(AdminContactRequestResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
