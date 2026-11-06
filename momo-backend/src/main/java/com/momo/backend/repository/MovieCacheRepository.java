package com.momo.backend.repository;

import com.momo.backend.entity.MovieCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

public interface MovieCacheRepository
        extends JpaRepository<MovieCache, Long> {

    Optional<MovieCache> findByCacheTypeAndCacheKey(
            String cacheType,
            String cacheKey
    );

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO movie_cache
                (cache_type, cache_key, content_json, cached_at, expires_at)
            VALUES (:cacheType, :cacheKey, :contentJson, :now, :expiresAt)
            ON DUPLICATE KEY UPDATE
                content_json = VALUES(content_json),
                cached_at = VALUES(cached_at),
                expires_at = VALUES(expires_at)
            """, nativeQuery = true)
    void upsert(
            @Param("cacheType") String cacheType,
            @Param("cacheKey") String cacheKey,
            @Param("contentJson") String contentJson,
            @Param("now") LocalDateTime now,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    @Modifying
    @Transactional
    @Query("delete from MovieCache cache where cache.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}