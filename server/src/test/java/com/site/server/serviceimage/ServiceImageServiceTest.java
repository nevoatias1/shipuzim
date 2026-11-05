package com.site.server.serviceimage;

import com.site.server.gallery.ImageSanitizer;
import com.site.server.gallery.storage.ImageStorage;
import com.site.server.gallery.storage.ImageStorageException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceImageServiceTest {
    private final ServiceImageRepository repository = mock(ServiceImageRepository.class);
    private final ImageSanitizer sanitizer = mock(ImageSanitizer.class);
    private final ImageStorage storage = mock(ImageStorage.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final ServiceImageService service = new ServiceImageService(repository, sanitizer, storage, manager);
    private final byte[] sanitized = {1, 2, 3};
    private final MockMultipartFile file = new MockMultipartFile("file", new byte[]{4, 5, 6});

    @BeforeEach
    void prepare() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(sanitizer.sanitize(file)).thenReturn(sanitized);
        when(storage.store(sanitized)).thenReturn(new ImageStorage.StoredImage("/uploads/new.png", "new.png"));
        when(repository.findForUpdate(ServiceKey.PAINTING)).thenReturn(Optional.of(
                new ServiceImage(ServiceKey.PAINTING, "/uploads/old.png", "old.png")));
    }

    @Test
    void replacementDeletesOldImageOnlyAfterDatabaseCommit() {
        assertThat(service.put(ServiceKey.PAINTING, file)).containsEntry("imageUrl", "/uploads/new.png");
        var order = inOrder(sanitizer, storage, repository, manager);
        order.verify(sanitizer).sanitize(file);
        order.verify(storage).store(sanitized);
        order.verify(repository).saveAndFlush(any());
        order.verify(manager).commit(any());
        order.verify(storage).delete("old.png");
        verify(storage, never()).delete("new.png");
    }

    @Test
    void failedCommitCleansNewUploadAndKeepsOldStoredImage() {
        doThrow(new DataAccessResourceFailureException("synthetic failure")).when(manager).commit(any());
        assertThatThrownBy(() -> service.put(ServiceKey.PAINTING, file)).isInstanceOf(DataAccessResourceFailureException.class);
        verify(storage).delete("new.png");
        verify(storage, never()).delete("old.png");
    }

    @Test
    void failedOldFileCleanupDoesNotUndoSuccessfulReplacement() {
        doThrow(new ImageStorageException()).when(storage).delete("old.png");
        assertThat(service.put(ServiceKey.PAINTING, file)).containsEntry("imageUrl", "/uploads/new.png");
        verify(storage, never()).delete("new.png");
    }

    @Test
    void failedStorageDeletionLeavesDatabaseRowAvailable() {
        doThrow(new ImageStorageException()).when(storage).delete("old.png");
        assertThatThrownBy(() -> service.delete(ServiceKey.PAINTING)).isInstanceOf(ImageStorageException.class);
        verify(repository, never()).delete(any());
    }
}
