package com.site.server.gallery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GalleryUpdateRequest(
        @Size(max = 200, message = "Caption must be at most 200 characters") String caption,
        @NotNull(message = "Category is required") GalleryCategory category,
        @NotNull(message = "Sort order is required") Integer sortOrder) {
    public GalleryUpdateRequest {
        caption = caption == null ? null : caption.trim();
    }

    @Override
    public String toString() { return "GalleryUpdateRequest[redacted]"; }
}
