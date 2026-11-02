
package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "movie_poster_cache")
public class MoviePosterCache {

    // 영화 고유 코드
    @Id
    @Column(length = 20)
    private String movieCode;

    // 영화 제목
    @Column(nullable = false)
    private String title;

    // 영화 개봉일
    private String releaseDate;

    // 포스터 이미지 주소
    @Column(length = 1000)
    private String posterUrl;

    // 포스터 조회 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PosterStatus status;

    // 마지막 수정 시간
    private LocalDateTime updatedAt;

    // 포스터 캐시 생성
    public MoviePosterCache(
            String movieCode,
            String title,
            String releaseDate
    ) {
        this.movieCode = movieCode;
        this.title = title;
        this.releaseDate = releaseDate;
        this.status = PosterStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    // 포스터 조회 성공
    public void updatePoster(String posterUrl) {
        this.posterUrl = posterUrl;
        this.status = PosterStatus.SUCCESS;
        this.updatedAt = LocalDateTime.now();
    }

    // 포스터 조회 실패
    public void markAsFailed() {
        this.posterUrl = null;
        this.status = PosterStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    // 포스터 조회 시작
    public void markAsProcessing() {
        this.status = PosterStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }
}
