package com.site.server.gallery.storage;

public interface ImageStorage {
    StoredImage store(byte[] sanitizedPng);
    void delete(String storageKey);

    record StoredImage(String imageUrl, String storageKey) {
    }
}
