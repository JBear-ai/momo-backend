package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "user_favorite_movies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_favorite_movie",
                        columnNames = {"user_id", "movie_code"}
                )
        },
        indexes = {
                @Index(name = "ix_user_favorite_created", columnList = "user_id, created_at, favorite_id")
        }
)
public class FavoriteMovie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // KOBIS 영화 코드를 기준으로 회원별 관심 영화를 구분한다.
    // 영화 상세 정보가 아직 movies 테이블에 저장되지 않은 경우도 있으므로 FK는 두지 않는다.
    @Column(name = "movie_code", nullable = false, length = 8)
    private String movieCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "release_date", length = 20)
    private String releaseDate;

    @Column(name = "poster_url", length = 1000)
    private String posterUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FavoriteMovie(User user, String movieCode, String title,
                         String releaseDate, String posterUrl) {
        this.user = user;
        this.movieCode = movieCode;
        updateMovieSnapshot(title, releaseDate, posterUrl);
    }

    public void updateMovieSnapshot(String title, String releaseDate, String posterUrl) {
        if (title != null && !title.isBlank()) this.title = title.trim();
        if (releaseDate != null && !releaseDate.isBlank()) this.releaseDate = releaseDate.trim();
        if (posterUrl != null && !posterUrl.isBlank()) this.posterUrl = posterUrl.trim();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
