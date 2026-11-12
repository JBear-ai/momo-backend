
package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_user_movie",
                        columnNames = {"user_id", "movie_code"}
                )
        },
        indexes = {
                @Index(name = "ix_review_created_at_id", columnList = "created_at, review_id"),
                @Index(name = "ix_review_movie_created_at", columnList = "movie_code, created_at, review_id"),
                @Index(name = "ix_review_user_created_at", columnList = "user_id, created_at, review_id")
        }
)
@Getter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "movie_code", nullable = false, length = 20)
    private String movieCode;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Review(User user, String movieCode, String title, String content) {
        this.user = user;
        this.movieCode = movieCode;
        this.title = title;
        this.content = content;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateReview(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
