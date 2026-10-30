package com.site.server.gallery.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.gallery.storage", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {
    private final Path root;

    public LocalImageStorage(@Value("${app.gallery.local-directory:./uploads}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public StoredImage store(byte[] sanitizedPng) {
        String key = UUID.randomUUID() + ".png";
        Path destination = pathFor(key);
        try {
            Files.createDirectories(root);
            Files.write(destination, sanitizedPng, StandardOpenOption.CREATE_NEW);
            return new StoredImage("/uploads/" + key, key);
        } catch (IOException exception) {
            try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
            throw new ImageStorageException();
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(pathFor(storageKey));
        } catch (IOException exception) {
            throw new ImageStorageException();
        }
    }

    private Path pathFor(String key) {
        if (key == null || !key.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.png")) {
            throw new ImageStorageException();
        }
        Path path = root.resolve(key).normalize();
        if (!path.getParent().equals(root)) throw new ImageStorageException();
        return path;
    }

    public Path getRoot() { return root; }
}
