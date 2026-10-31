package com.site.server;

import com.site.server.contact.ContactRequest;
import com.site.server.contact.ContactRequestRepository;
import com.site.server.contact.WorkType;
import com.site.server.gallery.GalleryCategory;
import com.site.server.gallery.GalleryItem;
import com.site.server.gallery.GalleryItemRepository;
import com.site.server.model.CustomUser;
import com.site.server.model.Role;
import com.site.server.repository.UserRepository;
import com.site.server.review.Review;
import com.site.server.review.ReviewRepository;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired private Flyway flyway;
    @Autowired private ContactRequestRepository contactRequests;
    @Autowired private UserRepository users;
    @Autowired private GalleryItemRepository galleryItems;
    @Autowired private ReviewRepository reviews;

    @Test
    void postgresMigrationsAndPersistenceWork() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");

        String suffix = UUID.randomUUID().toString();
        ContactRequest contact = contactRequests.saveAndFlush(new ContactRequest(
                "Postgres QA", "0501234567", "qa@example.test", "Haifa", WorkType.CLADDING, "Migration smoke test"));
        ContactRequest loadedContact = contactRequests.findById(contact.getId()).orElseThrow();
        assertThat(loadedContact.getFullName()).isEqualTo("Postgres QA");
        assertThat(loadedContact.getStatus().name()).isEqualTo("NEW");

        String username = "postgres-qa-" + suffix;
        CustomUser user = new CustomUser("Postgres", "QA", username + "@example.test", null, null,
                username, "synthetic-test-password", Role.USER);
        assertThat(users.register(user)).isEqualTo("User registered successfully");
        assertThat(users.findUserByUsername(username).getEmail()).isEqualTo(username + "@example.test");

        GalleryItem galleryItem = galleryItems.saveAndFlush(new GalleryItem(
                "https://images.example.test/qa.png", "postgres-qa-" + suffix, "QA image", GalleryCategory.CLADDING));
        assertThat(galleryItems.findById(galleryItem.getId()).orElseThrow().getCaption()).isEqualTo("QA image");

        Review review = reviews.saveAndFlush(new Review(username, 5, "Postgres cascade check"));
        assertThat(reviews.findById(review.getId())).isPresent();
        users.deleteUser(username);
        assertThat(reviews.findById(review.getId())).isEmpty();
    }
}
