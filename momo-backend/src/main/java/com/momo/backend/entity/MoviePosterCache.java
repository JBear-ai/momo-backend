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

    // 현재 비동기 작업의 소유권 토큰
    @Column(length = 36)
    private String claimToken;

    // 마지막 수정 시간
    private LocalDateTime updatedAt;

    public void updatePosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
        this.updatedAt = LocalDateTime.now();
    }

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


}