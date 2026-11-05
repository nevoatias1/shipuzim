package com.site.server.serviceimage;

import com.site.server.gallery.ImageSanitizer;
import com.site.server.gallery.storage.ImageStorage;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiceImageService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceImageService.class);
    private final ServiceImageRepository repository;
    private final ImageSanitizer sanitizer;
    private final ImageStorage storage;
    private final TransactionTemplate transactions;

    public ServiceImageService(ServiceImageRepository repository, ImageSanitizer sanitizer, ImageStorage storage,
                               PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.sanitizer = sanitizer;
        this.storage = storage;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public Map<ServiceKey, String> list() {
        var images = new EnumMap<ServiceKey, String>(ServiceKey.class);
        repository.findAll().forEach(image -> images.put(image.getId(), image.getImageUrl()));
        return images;
    }

    public Map<String, String> put(ServiceKey key, MultipartFile file) {
        var stored = storage.store(sanitizer.sanitize(file));
        String previousKey;
        try {
            previousKey = transactions.execute(status -> {
                var previous = repository.findForUpdate(key);
                String oldKey = previous.map(ServiceImage::getStorageKey).orElse(null);
                var image = previous.orElseGet(() -> new ServiceImage(key, stored.imageUrl(), stored.storageKey()));
                image.replace(stored.imageUrl(), stored.storageKey());
                repository.saveAndFlush(image);
                return oldKey;
            });
        } catch (RuntimeException failure) {
            cleanup(stored.storageKey());
            throw failure;
        }
        // Delete the old file only after the replacement database transaction commits.
        if (previousKey != null) cleanup(previousKey);
        return Map.of("imageUrl", stored.imageUrl());
    }

    @Transactional
    public void delete(ServiceKey key) {
        var image = repository.findForUpdate(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // As with gallery deletion, storage failure leaves the row available for retry.
        storage.delete(image.getStorageKey());
        repository.delete(image);
    }

    private void cleanup(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException failure) {
            logger.warn("Service image storage cleanup failed.");
        }
    }
}
