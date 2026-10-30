package com.site.server.contact;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactPrivacyTest {
    @Test
    void stringRepresentationsCannotRevealRequestContentsInFrameworkLogs() {
        ContactRequestDto dto = new ContactRequestDto("Private Name", "0501234567",
                "private@example.com", "Private City", WorkType.CLADDING, "Private Description", "Private Website");
        ContactRequestCreated event = new ContactRequestCreated(dto.workType(), dto.fullName(),
                dto.phone(), dto.city(), dto.description(), LocalDateTime.of(2026, 1, 1, 12, 0));

        assertThat(dto.toString()).isEqualTo("ContactRequestDto[redacted]");
        assertThat(event.toString()).isEqualTo("ContactRequestCreated[redacted]");
    }
}
