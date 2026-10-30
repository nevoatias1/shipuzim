package com.site.server.contact;

import java.time.LocalDateTime;

public record ContactRequestCreated(WorkType workType, String fullName, String phone,
                                    String city, String description, LocalDateTime createdAt) {
    @Override
    public String toString() {
        return "ContactRequestCreated[redacted]";
    }
}
