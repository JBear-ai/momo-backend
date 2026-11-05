package com.momo.backend.repository;

import com.momo.backend.entity.MoviePosterCache;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.Collection;
import java.util.List;

// 영화 포스터 캐시 조회 및 저장
public interface MoviePosterCacheRepository
        extends JpaRepository<MoviePosterCache, String> {

    // 여러 영화 코드에 해당하는 포스터 캐시를 한 번에 조회
    List<MoviePosterCache> findAllByMovieCodeIn(
            Collection<String> movieCodes
    );
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO movie_poster_cache
                (movie_code, title, release_date, poster_url, status, updated_at)
            VALUES (:movieCode, :title, :releaseDate, NULL, 'PENDING', :now)
            ON DUPLICATE KEY UPDATE title = VALUES(title), release_date = VALUES(release_date)
            """, nativeQuery = true)
    void insertIfAbsent(@Param("movieCode") String movieCode, @Param("title") String title,
                        @Param("releaseDate") String releaseDate, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE movie_poster_cache
               SET status = 'PROCESSING', updated_at = :now
             WHERE movie_code = :movieCode
               AND (status = 'PENDING'
                    OR (status = 'FAILED' AND updated_at < :failedBefore)
                    OR (status = 'PROCESSING'
                        AND (updated_at IS NULL OR updated_at < :processingBefore)))
            """, nativeQuery = true)
    int tryClaim(@Param("movieCode") String movieCode, @Param("now") LocalDateTime now,
                 @Param("failedBefore") LocalDateTime failedBefore,
                 @Param("processingBefore") LocalDateTime processingBefore);
}