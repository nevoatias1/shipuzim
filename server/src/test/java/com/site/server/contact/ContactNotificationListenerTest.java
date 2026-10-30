package com.site.server.contact;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ContactNotificationListenerTest {
    @Test
    void fullExecutorCannotPropagateFailureToTheCommittedRequest() {
        NotificationService notifications = mock(NotificationService.class);
        ContactNotificationListener listener = new ContactNotificationListener(notifications, task -> {
            throw new TaskRejectedException("executor full");
        });
        ContactRequestCreated event = new ContactRequestCreated(WorkType.PAINTING,
                "Test Name", "0501234567", null, null, LocalDateTime.of(2026, 1, 1, 12, 0));

        assertThatCode(() -> listener.afterCommit(event)).doesNotThrowAnyException();
        verifyNoInteractions(notifications);
    }
}
