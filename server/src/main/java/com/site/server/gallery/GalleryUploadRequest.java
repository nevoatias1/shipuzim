package com.site.server.gallery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record GalleryUploadRequest(
        @NotNull(message = "Image is required") MultipartFile file,
        @Size(max = 200, message = "Caption must be at most 200 characters") String caption,
        @NotNull(message = "Category is required") GalleryCategory category) {
    public GalleryUploadRequest {
        caption = caption == null ? null : caption.trim();
    }

    @Override
    public String toString() { return "GalleryUploadRequest[redacted]"; }
}
