package com.site.server.review;

import java.time.LocalDateTime;

public record AdminReviewResponse(Long id, String displayName, String authorEmail, int rating, String text,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static AdminReviewResponse from(ReviewRepository.ReviewRow row) {
        return new AdminReviewResponse(row.getId(), ReviewResponse.displayName(row.getFirstName(), row.getLastName()),
                row.getEmail(), row.getRating(), row.getText(), row.getCreatedAt(), row.getUpdatedAt());
    }

    @Override public String toString() { return "AdminReviewResponse[redacted]"; }
}
