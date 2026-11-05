package com.site.server.serviceimage;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "service_image")
public class ServiceImage implements Persistable<ServiceKey> {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "service_key", length = 40)
    private ServiceKey key;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private boolean newRow = true;

    protected ServiceImage() { }

    public ServiceImage(ServiceKey key, String imageUrl, String storageKey) {
        this.key = key;
        replace(imageUrl, storageKey);
    }

    public void replace(String imageUrl, String storageKey) {
        this.imageUrl = imageUrl;
        this.storageKey = storageKey;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PostLoad
    @PostPersist
    void markPersisted() { newRow = false; }

    @Override public ServiceKey getId() { return key; }
    @Override public boolean isNew() { return newRow; }
    public String getImageUrl() { return imageUrl; }
    public String getStorageKey() { return storageKey; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
