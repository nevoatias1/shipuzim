package com.site.server.gallery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "gallery_item")
public class GalleryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(length = 200)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GalleryCategory category;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected GalleryItem() {
    }

    public GalleryItem(String imageUrl, String storageKey, String caption, GalleryCategory category) {
        this.imageUrl = imageUrl;
        this.storageKey = storageKey;
        this.caption = caption;
        this.category = category;
    }

    @PrePersist
    void setCreatedAtOnCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void update(String caption, GalleryCategory category, int sortOrder) {
        this.caption = caption;
        this.category = category;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getStorageKey() { return storageKey; }
    public String getCaption() { return caption; }
    public GalleryCategory getCategory() { return category; }
    public int getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
