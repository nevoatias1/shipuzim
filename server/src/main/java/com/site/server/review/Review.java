package com.site.server.review;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "review")
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 255)
    private String username;
    @Column(nullable = false)
    private int rating;
    @Column(nullable = false, length = 500)
    private String text;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Review() { }

    public Review(String username, int rating, String text) {
        this.username = username;
        update(rating, text);
        this.createdAt = this.updatedAt;
    }

    public void update(int rating, String text) {
        this.rating = rating;
        this.text = text;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public int getRating() { return rating; }
    public String getText() { return text; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
