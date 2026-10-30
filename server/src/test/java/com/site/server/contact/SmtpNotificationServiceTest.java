package com.site.server.contact;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpNotificationServiceTest {
    @Test
    void sendsHebrewPlainTextWithQuoteDetailsAndConfiguredAddresses() {
        JavaMailSender sender = mock(JavaMailSender.class);
        SmtpNotificationService notifications = new SmtpNotificationService(
                sender, "site@example.com", "business@example.com");
        notifications.notifyNewRequest(new ContactRequestCreated(WorkType.BOTH,
                "שם לדוגמה", "0501234567", "תל אביב", "קיר פנימי", LocalDateTime.of(2026, 1, 1, 12, 0)));

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("site@example.com");
        assertThat(message.getValue().getTo()).containsExactly("business@example.com");
        assertThat(message.getValue().getSubject()).isEqualTo("בקשה חדשה להצעת מחיר");
        assertThat(message.getValue().getText()).contains("חיפוי קירות וצביעה", "שם לדוגמה",
                "0501234567", "תל אביב", "קיר פנימי", "2026-01-01T12:00", "UTC");
    }
}
