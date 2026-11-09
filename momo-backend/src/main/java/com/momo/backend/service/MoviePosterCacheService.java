package com.momo.backend.service;

import com.momo.backend.dto.movie.MoviePosterStatusResponse;
import com.momo.backend.entity.MoviePosterCache;
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

    private static final long FAILED_RETRY_HOURS = 6;
    private static final long PROCESSING_TIMEOUT_MINUTES = 10;

    public Optional<MoviePosterCache> findCache(String movieCode) {
        return posterCacheRepository.findById(movieCode);
    }

    @Transactional
    public MoviePosterCache createCache(String movieCode, String title, String releaseDate) {
        posterCacheRepository.insertIfAbsent(movieCode, title, releaseDate, LocalDateTime.now());
        return posterCacheRepository.findById(movieCode)
                .orElseThrow(() -> new IllegalStateException("포스터 캐시 생성 실패: " + movieCode));
    }

    @Transactional
    public boolean tryMarkAsProcessing(String movieCode) {
        LocalDateTime now = LocalDateTime.now();
        return posterCacheRepository.tryClaim(
                movieCode,
                now,
                now.minusHours(FAILED_RETRY_HOURS),
                now.minusMinutes(PROCESSING_TIMEOUT_MINUTES)
        ) == 1;
    }

    @Transactional
    public void releaseProcessingClaim(String movieCode) {
        posterCacheRepository.releaseClaim(movieCode, LocalDateTime.now());
    }

    @Transactional
    public void updatePoster(String movieCode, String posterUrl) {
        posterCacheRepository.findById(movieCode).ifPresent(cache -> cache.updatePoster(posterUrl));
    }

    @Transactional
    public void markAsFailed(String movieCode) {
        posterCacheRepository.findById(movieCode).ifPresent(MoviePosterCache::markAsFailed);
    }

    public List<MoviePosterStatusResponse> findPosterStatuses(Collection<String> movieCodes) {
        if (movieCodes == null || movieCodes.isEmpty()) {
            return List.of();
        }

        return posterCacheRepository.findAllByMovieCodeIn(movieCodes).stream()
                .map(cache -> new MoviePosterStatusResponse(
                        cache.getMovieCode(),
                        cache.getStatus(),
                        cache.getPosterUrl() == null ? "" : cache.getPosterUrl()
                ))
                .toList();
    }

    public Optional<MoviePosterStatusResponse> findPosterStatus(String movieCode) {
        return posterCacheRepository.findById(movieCode)
                .map(cache -> new MoviePosterStatusResponse(
                        cache.getMovieCode(),
                        cache.getStatus(),
                        cache.getPosterUrl() == null ? "" : cache.getPosterUrl()
                ));
    }
}
