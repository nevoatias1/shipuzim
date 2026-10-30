package com.site.server.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 500) String text) {
    public ReviewRequest {
        text = text == null ? "" : text.replaceAll("[\\p{Cc}\\p{Cf}]", "").strip();
    }

    @Override public String toString() { return "ReviewRequest[redacted]"; }
}
