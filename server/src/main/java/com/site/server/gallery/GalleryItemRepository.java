package com.site.server.gallery;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryItemRepository extends JpaRepository<GalleryItem, Long> {
}
