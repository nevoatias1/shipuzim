package com.site.server.serviceimage;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceImageRepository extends JpaRepository<ServiceImage, ServiceKey> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select image from ServiceImage image where image.key = :key")
    Optional<ServiceImage> findForUpdate(@Param("key") ServiceKey key);
}
