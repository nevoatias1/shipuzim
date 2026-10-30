package com.site.server.controller;

import com.site.server.review.AdminReviewResponse;
import com.site.server.review.ReviewPage;
import com.site.server.review.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reviews")
public class AdminReviewController {
    private final ReviewService service;

    public AdminReviewController(ReviewService service) { this.service = service; }

    @GetMapping
    public ReviewPage<AdminReviewResponse> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.listForAdmin(page, size);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
