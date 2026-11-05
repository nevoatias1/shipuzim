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
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "PERGOLA_FIXED,פרגולות קבועות", "PERGOLA_ELECTRIC,פרגולות חשמליות", "BALCONY_ENCLOSURE,סגירת מרפסות"})
    void newWorkTypesHaveHebrewEmailLabels(WorkType type, String label) {
        JavaMailSender sender = mock(JavaMailSender.class);
        new SmtpNotificationService(sender, "site@example.test", "business@example.test")
                .notifyNewRequest(new ContactRequestCreated(type, "Test", "0501234567", null, null, LocalDateTime.now()));
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getText()).contains("סוג עבודה: " + label);
    }

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
        assertThat(message.getValue().getText()).contains("חיפוי וצביעה", "שם לדוגמה",
                "0501234567", "תל אביב", "קיר פנימי", "2026-01-01T12:00", "UTC");
    }
}
