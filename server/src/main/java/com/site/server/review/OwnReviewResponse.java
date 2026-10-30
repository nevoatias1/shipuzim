package com.site.server.review;

import java.time.LocalDateTime;

public record OwnReviewResponse(Long id, int rating, String text, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static OwnReviewResponse from(Review review) {
        return new OwnReviewResponse(review.getId(), review.getRating(), review.getText(),
                review.getCreatedAt(), review.getUpdatedAt());
    }

    @Override public String toString() { return "OwnReviewResponse[redacted]"; }
}
