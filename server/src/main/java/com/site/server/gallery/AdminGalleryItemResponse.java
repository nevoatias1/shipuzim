package com.site.server.gallery;

import java.time.LocalDateTime;

public record AdminGalleryItemResponse(Long id, String imageUrl, String caption, GalleryCategory category,
                                       int sortOrder, LocalDateTime createdAt) {
    public static AdminGalleryItemResponse from(GalleryItem item) {
        return new AdminGalleryItemResponse(item.getId(), item.getImageUrl(), item.getCaption(),
                item.getCategory(), item.getSortOrder(), item.getCreatedAt());
    }

    @Override
    public String toString() { return "AdminGalleryItemResponse[redacted]"; }
}
