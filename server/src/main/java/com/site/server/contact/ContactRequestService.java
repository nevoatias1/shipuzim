package com.site.server.contact;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactRequestService {
    private final ContactRequestRepository repository;
    private final ApplicationEventPublisher events;

    public ContactRequestService(ContactRequestRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public ContactRequest create(ContactRequestDto request) {
        ContactRequest entity = new ContactRequest(
                request.fullName(),
                request.normalizedPhone(),
                request.email(),
                request.city(),
                request.workType(),
                request.description()
        );
        ContactRequest saved = repository.save(entity);
        events.publishEvent(new ContactRequestCreated(saved.getWorkType(), saved.getFullName(),
                saved.getPhone(), saved.getCity(), saved.getDescription(), saved.getCreatedAt()));
        return saved;
    }
}
