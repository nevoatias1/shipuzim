package com.site.server.contact;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactRequestService {
    private final ContactRequestRepository repository;

    public ContactRequestService(ContactRequestRepository repository) {
        this.repository = repository;
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
        return repository.save(entity);
    }
}