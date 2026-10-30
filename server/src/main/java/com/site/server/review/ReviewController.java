package com.site.server.review;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) { this.service = service; }

    @GetMapping("/api/reviews")
    public ReviewPage<ReviewResponse> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @GetMapping("/users/review")
    public OwnReviewResponse own(Principal principal) { return service.own(principal.getName()); }

    @PutMapping("/users/review")
    public OwnReviewResponse save(Principal principal, @Valid @RequestBody ReviewRequest request) {
        return service.saveOwn(principal.getName(), request);
    }

    @DeleteMapping("/users/review")
    public ResponseEntity<Void> deleteOwn(Principal principal) {
        service.deleteOwn(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
