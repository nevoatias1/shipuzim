package com.site.server.contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ContactNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(ContactNotificationListener.class);
    private final NotificationService notifications;
    private final TaskExecutor executor;

    public ContactNotificationListener(NotificationService notifications,
            @Qualifier("contactNotificationExecutor") TaskExecutor executor) {
        this.notifications = notifications;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(ContactRequestCreated request) {
        try {
            executor.execute(() -> {
                try {
                    notifications.notifyNewRequest(request);
                } catch (Exception exception) {
                    // Exception messages and mail payloads can contain personal data.
                    log.warn("Contact notification failed");
                }
            });
        } catch (RuntimeException exception) {
            log.warn("Contact notification could not be scheduled");
        }
    }
}
