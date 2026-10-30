package com.site.server.contact;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpNotificationService implements NotificationService {
    @Override
    public void notifyNewRequest(ContactRequestCreated request) {
        // Mail is deliberately disabled until configured.
    }
}
