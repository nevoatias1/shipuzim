package com.site.server.gallery;

public record GalleryItemResponse(Long id, String imageUrl, String caption, GalleryCategory category) {
    public static GalleryItemResponse from(GalleryItem item) {
        return new GalleryItemResponse(item.getId(), item.getImageUrl(), item.getCaption(), item.getCategory());
    }

    @Override
    public String toString() { return "GalleryItemResponse[redacted]"; }
}
