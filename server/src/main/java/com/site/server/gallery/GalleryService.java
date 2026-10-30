package com.site.server.gallery;

import com.site.server.gallery.storage.ImageStorage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GalleryService {
    private static final Logger logger = LoggerFactory.getLogger(GalleryService.class);
    private final GalleryItemRepository repository;
    private final ImageStorage storage;
    private final ImageSanitizer sanitizer;
    private final TransactionTemplate transactions;

    public GalleryService(GalleryItemRepository repository, ImageStorage storage, ImageSanitizer sanitizer,
                          PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.storage = storage;
        this.sanitizer = sanitizer;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<GalleryItemResponse> list() {
        Sort order = Sort.by("sortOrder").ascending().and(Sort.by("createdAt", "id").descending());
        return repository.findAll(order).stream().map(GalleryItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminGalleryItemResponse> listForAdmin() {
        Sort order = Sort.by("sortOrder").ascending().and(Sort.by("createdAt", "id").descending());
        return repository.findAll(order).stream().map(AdminGalleryItemResponse::from).toList();
    }

    public GalleryItemResponse create(GalleryUploadRequest request) {
        byte[] image = sanitizer.sanitize(request.file());
        ImageStorage.StoredImage stored = storage.store(image);
        try {
            // Execute through commit here so a failed database write can clean up its uploaded image.
            return transactions.execute(status -> GalleryItemResponse.from(repository.saveAndFlush(
                    new GalleryItem(stored.imageUrl(), stored.storageKey(), request.caption(), request.category()))));
        } catch (RuntimeException exception) {
            try {
                storage.delete(stored.storageKey());
            } catch (RuntimeException cleanupFailure) {
                logger.warn("Gallery upload cleanup failed.");
            }
            throw exception;
        }
    }

    @Transactional
    public GalleryItemResponse update(long id, GalleryUpdateRequest request) {
        GalleryItem item = find(id);
        item.update(request.caption(), request.category(), request.sortOrder());
        return GalleryItemResponse.from(item);
    }

    @Transactional
    public void delete(long id) {
        GalleryItem item = find(id);
        // A storage failure aborts the transaction and leaves the database row available for retry.
        storage.delete(item.getStorageKey());
        repository.delete(item);
    }

    private GalleryItem find(long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
