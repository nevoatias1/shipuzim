package com.site.server.review;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByUsername(String username);

    @Query(value = """
            SELECT r.id, r.rating, r.text, r.created_at AS "createdAt", r.updated_at AS "updatedAt",
                   u.first_name AS "firstName", u.last_name AS "lastName", u.email
            FROM review r JOIN users u ON u.username = r.username
            ORDER BY r.created_at DESC, r.id DESC
            """, countQuery = "SELECT COUNT(*) FROM review", nativeQuery = true)
    Page<ReviewRow> listWithAuthors(Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double averageRating();

    interface ReviewRow {
        Long getId();
        int getRating();
        String getText();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
        String getFirstName();
        String getLastName();
        String getEmail();
    }
}
