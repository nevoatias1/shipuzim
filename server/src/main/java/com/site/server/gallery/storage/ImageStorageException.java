package com.site.server.gallery.storage;

public class ImageStorageException extends RuntimeException {
    public ImageStorageException() {
        super("Image storage operation failed.");
    }
}
