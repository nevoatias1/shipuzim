package com.site.server.gallery;

import com.site.server.gallery.storage.ImageStorage;
import com.site.server.gallery.storage.ImageStorageException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GalleryServiceTest {
    @Test
    void failedDatabaseCommitCleansUpUploadedImage() {
        var repository = mock(GalleryItemRepository.class);
        var storage = mock(ImageStorage.class);
        var sanitizer = mock(ImageSanitizer.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        byte[] image = {1, 2, 3};
        when(sanitizer.sanitize(any())).thenReturn(image);
        when(storage.store(image)).thenReturn(new ImageStorage.StoredImage("/uploads/key.png", "key.png"));
        when(repository.saveAndFlush(any())).thenReturn(new GalleryItem("/uploads/key.png", "key.png", null, GalleryCategory.CLADDING));
        doThrow(new DataAccessResourceFailureException("synthetic commit failure")).when(manager).commit(any());
        var service = new GalleryService(repository, storage, sanitizer, manager);
        assertThatThrownBy(() -> service.create(new GalleryUploadRequest(
                new MockMultipartFile("file", image), null, GalleryCategory.CLADDING)))
                .isInstanceOf(DataAccessResourceFailureException.class);
        verify(storage).delete("key.png");
    }

    @Test
    void failedStorageDeletionDoesNotDeleteTheDatabaseRow() {
        var repository = mock(GalleryItemRepository.class);
        var storage = mock(ImageStorage.class);
        var item = new GalleryItem("/uploads/key.png", "key.png", null, GalleryCategory.CLADDING);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        doThrow(new ImageStorageException()).when(storage).delete("key.png");
        var service = new GalleryService(repository, storage, mock(ImageSanitizer.class), mock(PlatformTransactionManager.class));
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ImageStorageException.class);
        verify(repository, never()).delete(any());
    }
}
