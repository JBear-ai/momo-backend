
package com.momo.backend.service;

import com.momo.backend.dto.movie.MoviePosterStatusResponse;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MoviePosterCacheRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoviePosterCacheService {

    private final MoviePosterCacheRepository posterCacheRepository;

    // =========================================================
    // 1. 영화 포스터 캐시 단건 조회
    // =========================================================

    public Optional<MoviePosterCache> findCache(String movieCode) {
        return posterCacheRepository.findById(movieCode);
    }

    // =========================================================
    // 2. 영화 포스터 캐시 생성
    // =========================================================

    @Transactional
    public synchronized MoviePosterCache createCache(
            String movieCode,
            String title,
            String releaseDate
    ) {

        Optional<MoviePosterCache> existingCache =
                posterCacheRepository.findById(movieCode);

        if (existingCache.isPresent()) {
            return existingCache.get();
        }

        MoviePosterCache newCache = new MoviePosterCache(
                movieCode,
                title,
                releaseDate
        );

        return posterCacheRepository.save(newCache);
    }

    // =========================================================
    // 3. 포스터 조회 작업 시작
    // =========================================================

    @Transactional
    public synchronized boolean tryMarkAsProcessing(String movieCode) {

        MoviePosterCache cache = posterCacheRepository
                .findById(movieCode)
                .orElse(null);

        if (cache == null) {
            return false;
        }

        // PENDING 상태인 경우에만 작업 시작
        if (cache.getStatus() != PosterStatus.PENDING) {
            return false;
        }

        cache.markAsProcessing();

        return true;
    }

    // =========================================================
    // 4. 포스터 조회 진행 상태 변경
    // =========================================================

    @Transactional
    public synchronized void markAsProcessing(String movieCode) {

        posterCacheRepository.findById(movieCode)
                .ifPresent(MoviePosterCache::markAsProcessing);
    }

    // =========================================================
    // 5. 포스터 조회 성공 및 주소 저장
    // =========================================================

    @Transactional
    public void updatePoster(String movieCode, String posterUrl) {

        posterCacheRepository.findById(movieCode)
                .ifPresent(cache -> cache.updatePoster(posterUrl));
    }

    // =========================================================
    // 6. 포스터 조회 실패 처리
    // =========================================================

    @Transactional
    public void markAsFailed(String movieCode) {

        posterCacheRepository.findById(movieCode)
                .ifPresent(MoviePosterCache::markAsFailed);
    }

    // =========================================================
    // 7. 영화 포스터 상태 일괄 조회
    // =========================================================

    public List<MoviePosterStatusResponse> findPosterStatuses(
            Collection<String> movieCodes
    ) {

        if (movieCodes == null || movieCodes.isEmpty()) {
            return List.of();
        }

        return posterCacheRepository
                .findAllByMovieCodeIn(movieCodes)
                .stream()
                .map(cache -> new MoviePosterStatusResponse(
                        cache.getMovieCode(),
                        cache.getStatus(),
                        cache.getPosterUrl() == null
                                ? ""
                                : cache.getPosterUrl()
                ))
                .toList();
    }

    // =========================================================
    // 8. 영화 포스터 상태 단건 조회
    // =========================================================

    public Optional<MoviePosterStatusResponse> findPosterStatus(
            String movieCode
    ) {

        return posterCacheRepository.findById(movieCode)
                .map(cache -> new MoviePosterStatusResponse(
                        cache.getMovieCode(),
                        cache.getStatus(),
                        cache.getPosterUrl() == null
                                ? ""
                                : cache.getPosterUrl()
                ));
    }
}
