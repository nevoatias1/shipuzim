package com.site.server.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminContactRequestService {
    private final ContactRequestRepository repository;

    public AdminContactRequestService(ContactRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<ContactRequest> list(int page, int size) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return repository.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional
    public void updateStatus(long id, RequestStatus status) {
        ContactRequest request = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        request.setStatus(status);
    }
}
