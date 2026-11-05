package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "movie_cache",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_movie_cache_type_key",
                        columnNames = {"cache_type", "cache_key"}
                )
        }
)
public class MovieCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 캐시 종류: SEARCH, DAILY_BOX_OFFICE, GENRE
    @Column(name = "cache_type", nullable = false, length = 30)
    private String cacheType;

    // 검색어, 날짜, 장르 등 캐시 식별값
    @Column(name = "cache_key", nullable = false, length = 500)
    private String cacheKey;

    // API 응답 데이터 JSON
    @Column(name = "content_json", nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    // 캐시 생성 시각
    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    // 캐시 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public MovieCache(
            String cacheType,
            String cacheKey,
            String contentJson,
            long ttlHours
    ) {
        this.cacheType = cacheType;
        this.cacheKey = cacheKey;
        update(contentJson, ttlHours);
    }

    // 캐시 유효성 검사
    public boolean isValid(LocalDateTime now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }

    // 기존 캐시 갱신
    public void update(String contentJson, long ttlHours) {
        LocalDateTime now = LocalDateTime.now();

        this.contentJson = contentJson;
        this.cachedAt = now;
        this.expiresAt = now.plusHours(ttlHours);
    }
}