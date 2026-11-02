
package com.momo.backend.service;

import com.momo.backend.dto.movie.MoviePosterStatusResponse;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MoviePosterCacheRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoviePosterCacheService {

    private final MoviePosterCacheRepository posterCacheRepository;

    // 포스터 캐시 단건 조회
    public Optional<MoviePosterCache> findCache(String movieCode) {
        return posterCacheRepository.findById(movieCode);
    }

    // 포스터 캐시 생성
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

    // 포스터 검색 작업 시작
    @Transactional
    public synchronized boolean tryMarkAsProcessing(String movieCode) {

        MoviePosterCache cache = posterCacheRepository
                .findById(movieCode)
                .orElse(null);

        if (cache == null) {
            return false;
        }

        PosterStatus status = cache.getStatus();

        // 처음 검색하는 영화
        boolean isPending = status == PosterStatus.PENDING;

        // 이전 검색에 실패한 영화
        boolean isFailed = status == PosterStatus.FAILED;

        // 서버 종료 등으로 10분 이상 처리 중인 영화
        boolean isStaleProcessing =
                status == PosterStatus.PROCESSING
                        && (
                        cache.getUpdatedAt() == null
                                || cache.getUpdatedAt().isBefore(
                                LocalDateTime.now().minusMinutes(10)
                        )
                );

        // 재검색 대상이 아니면 작업하지 않음
        if (!isPending && !isFailed && !isStaleProcessing) {
            return false;
        }

        // 처리 상태로 변경
        cache.markAsProcessing();

        return true;
    }

    // 포스터 조회 진행 상태 변경
    @Transactional
    public synchronized void markAsProcessing(String movieCode) {

        posterCacheRepository.findById(movieCode)
                .ifPresent(MoviePosterCache::markAsProcessing);
    }

    // 포스터 조회 성공 및 주소 저장
    @Transactional
    public void updatePoster(String movieCode, String posterUrl) {

        posterCacheRepository.findById(movieCode)
                .ifPresent(cache -> cache.updatePoster(posterUrl));
    }

    // 포스터 조회 실패 처리
    @Transactional
    public void markAsFailed(String movieCode) {

        posterCacheRepository.findById(movieCode)
                .ifPresent(MoviePosterCache::markAsFailed);
    }

    // 여러 영화의 포스터 상태 일괄 조회
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

    // 영화 포스터 상태 단건 조회
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
