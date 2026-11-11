package com.momo.backend.repository;

import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;

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

    // 재시도 대기열에서 오래된 작업부터 제한된 수만 가져온다.
    List<MoviePosterCache> findTop10ByStatusOrderByUpdatedAtAsc(PosterStatus status);

    List<MoviePosterCache> findTop10ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            PosterStatus status,
            LocalDateTime updatedBefore
    );

    // 여러 영화 코드에 해당하는 포스터 캐시를 한 번에 조회
    List<MoviePosterCache> findAllByMovieCodeIn(Collection<String> movieCodes);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO movie_poster_cache
                (movie_code, title, release_date, poster_url, status, updated_at)
            VALUES (:movieCode, :title, :releaseDate, NULL, 'PENDING', :now)
            ON DUPLICATE KEY UPDATE title = VALUES(title), release_date = VALUES(release_date)
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("movieCode") String movieCode,
            @Param("title") String title,
            @Param("releaseDate") String releaseDate,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE movie_poster_cache
               SET status = 'PROCESSING', claim_token = :claimToken, updated_at = :now
             WHERE movie_code = :movieCode
               AND (status = 'PENDING'
                    OR (status = 'FAILED' AND updated_at < :failedBefore)
                    OR (status = 'PROCESSING'
                        AND (updated_at IS NULL OR updated_at < :processingBefore)))
            """, nativeQuery = true)
    int tryClaim(
            @Param("movieCode") String movieCode,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now,
            @Param("failedBefore") LocalDateTime failedBefore,
            @Param("processingBefore") LocalDateTime processingBefore
    );

    // 비동기 작업이 스레드 풀에 등록되지 못한 경우 작업 점유 상태를 해제한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE movie_poster_cache
               SET status = 'PENDING', claim_token = NULL, updated_at = :now
             WHERE movie_code = :movieCode
               AND status = 'PROCESSING'
               AND claim_token = :claimToken
            """, nativeQuery = true)
    int releaseClaim(
            @Param("movieCode") String movieCode,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE movie_poster_cache
               SET poster_url = :posterUrl, status = 'SUCCESS',
                   claim_token = NULL, updated_at = :now
             WHERE movie_code = :movieCode
               AND status = 'PROCESSING'
               AND claim_token = :claimToken
            """, nativeQuery = true)
    int completeClaim(
            @Param("movieCode") String movieCode,
            @Param("claimToken") String claimToken,
            @Param("posterUrl") String posterUrl,
            @Param("now") LocalDateTime now
    );

    // 파일이 사라진 SUCCESS 캐시를 다시 수집 가능한 PENDING 상태로 되돌린다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE movie_poster_cache
               SET poster_url = NULL, status = 'PENDING',
                   claim_token = NULL, updated_at = :now
             WHERE movie_code = :movieCode
               AND (status <> 'PROCESSING'
                    OR updated_at IS NULL
                    OR updated_at < :processingBefore)
            """, nativeQuery = true)
    int resetMissingPoster(
            @Param("movieCode") String movieCode,
            @Param("now") LocalDateTime now,
            @Param("processingBefore") LocalDateTime processingBefore
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE movie_poster_cache
               SET poster_url = NULL, status = 'FAILED',
                   claim_token = NULL, updated_at = :now
             WHERE movie_code = :movieCode
               AND status = 'PROCESSING'
               AND claim_token = :claimToken
            """, nativeQuery = true)
    int failClaim(
            @Param("movieCode") String movieCode,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now
    );
}
