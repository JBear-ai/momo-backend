
package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "movie_ratings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_movie_rating_user_movie",
                        columnNames = {"user_id", "movie_code"}
                )
        },
        indexes = {
                @Index(name = "ix_movie_rating_movie_liked", columnList = "movie_code, liked"),
                @Index(name = "ix_movie_rating_user_liked", columnList = "user_id, liked, movie_code")
        }
)
public class MovieRating {

    // 영화 평가 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long ratingId;

    // 평가한 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 평가 대상 영화
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "movie_code",
            referencedColumnName = "movieCode",
            nullable = false
    )
    private Movie movie;

    // 평가 결과 (true: 좋아요, false: 싫어요)
    @Column(nullable = false)
    private Boolean liked;

    // 영화 평가 생성
    public MovieRating(User user, Movie movie, Boolean liked) {
        this.user = user;
        this.movie = movie;
        this.liked = liked;
    }

    // 기존 평가 변경
    public void updateRating(Boolean liked) {
        this.liked = liked;
    }
}
