package com.site.server.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {
    private final ReviewRepository repository;
    private final JdbcTemplate jdbc;

    public ReviewService(ReviewRepository repository, JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ReviewPage<ReviewResponse> list(int page, int size) {
        Page<ReviewRepository.ReviewRow> result = page(page, size);
        Double average = repository.averageRating();
        return new ReviewPage<>(result.map(ReviewResponse::from).getContent(), page, size, result.getTotalElements(),
                result.getTotalPages(), average == null ? 0 : average, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReviewPage<AdminReviewResponse> listForAdmin(int page, int size) {
        Page<ReviewRepository.ReviewRow> result = page(page, size);
        Double average = repository.averageRating();
        return new ReviewPage<>(result.map(AdminReviewResponse::from).getContent(), page, size, result.getTotalElements(),
                result.getTotalPages(), average == null ? 0 : average, result.getTotalElements());
    }

    private Page<ReviewRepository.ReviewRow> page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return repository.listWithAuthors(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public OwnReviewResponse own(String username) { return OwnReviewResponse.from(findOwn(username)); }

    @Transactional
    public OwnReviewResponse saveOwn(String username, ReviewRequest request) {
        // Serialize creates/edits for an identity, including when its first review does not yet exist.
        jdbc.queryForObject("SELECT username FROM users WHERE username = ? FOR UPDATE", String.class, username);
        Review review = repository.findByUsername(username).orElse(null);
        if (review == null) review = new Review(username, request.rating(), request.text());
        else review.update(request.rating(), request.text());
        return OwnReviewResponse.from(repository.saveAndFlush(review));
    }

    @Transactional
    public void deleteOwn(String username) { repository.delete(findOwn(username)); }

    @Transactional
    public void delete(long id) {
        repository.delete(repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    private Review findOwn(String username) {
        return repository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
