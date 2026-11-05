package com.site.server.contact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpNotificationService implements NotificationService {
    private final JavaMailSender sender;
    private final String from;
    private final String recipient;

    public SmtpNotificationService(JavaMailSender sender, @Value("${MAIL_FROM}") String from,
                                    @Value("${NOTIFY_TO}") String recipient) {
        Assert.hasText(from, "MAIL_FROM is required");
        Assert.hasText(recipient, "NOTIFY_TO is required");
        this.sender = sender;
        this.from = from;
        this.recipient = recipient;
    }

    @Override
    public void notifyNewRequest(ContactRequestCreated request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("בקשה חדשה להצעת מחיר");
        message.setText("""
                התקבלה בקשה חדשה להצעת מחיר

                סוג עבודה: %s
                שם: %s
                טלפון: %s
                עיר: %s
                תיאור: %s
                זמן יצירה (UTC): %s
                """.formatted(workType(request.workType()), request.fullName(), request.phone(),
                optional(request.city()), optional(request.description()), request.createdAt()));
        sender.send(message);
    }

    private String workType(WorkType value) {
        return switch (value) {
            case CLADDING -> "חיפוי קירות";
            case PAINTING -> "צביעה";
            case BOTH -> "חיפוי וצביעה";
            case PERGOLA_FIXED -> "פרגולות קבועות";
            case PERGOLA_ELECTRIC -> "פרגולות חשמליות";
            case BALCONY_ENCLOSURE -> "סגירת מרפסות";
            case OTHER -> "אחר";
        };
    }

    private String optional(String value) {
        return value == null ? "לא צוין" : value;
    }
}
