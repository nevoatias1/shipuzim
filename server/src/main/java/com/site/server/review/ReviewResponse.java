package com.site.server.review;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, String displayName, int rating, String text, LocalDateTime createdAt) {
    public static ReviewResponse from(ReviewRepository.ReviewRow row) {
        return new ReviewResponse(row.getId(), displayName(row.getFirstName(), row.getLastName()),
                row.getRating(), row.getText(), row.getCreatedAt());
    }

    public static String displayName(String firstName, String lastName) {
        String first = cleanName(firstName);
        String last = cleanName(lastName);
        return first + (last.isEmpty() || "-".equals(last) ? "" : " " + last.substring(0, last.offsetByCodePoints(0, 1)) + ".");
    }

    private static String cleanName(String value) {
        return value == null ? "" : value.replaceAll("[\\p{Cc}\\p{Cf}]", "").strip();
    }

    @Override public String toString() { return "ReviewResponse[redacted]"; }
}
