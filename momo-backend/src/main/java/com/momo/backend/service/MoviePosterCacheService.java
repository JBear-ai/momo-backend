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

    private static final long FAILED_RETRY_HOURS = 24;
    private static final long PROCESSING_TIMEOUT_MINUTES = 10;

    public Optional<MoviePosterCache> findCache(String movieCode) {
        return posterCacheRepository.findById(movieCode);
    }

    @Transactional
    public synchronized MoviePosterCache createCache(
            String movieCode,
            String title,
            String releaseDate
    ) {
        return posterCacheRepository.findById(movieCode)
                .orElseGet(() -> posterCacheRepository.save(
                        new MoviePosterCache(
                                movieCode,
                                title,
                                releaseDate
                        )
                ));
    }

    @Transactional
    public synchronized boolean tryMarkAsProcessing(String movieCode) {

        MoviePosterCache cache = posterCacheRepository
                .findById(movieCode)
                .orElse(null);

        if (cache == null) {
            return false;
        }

        PosterStatus status = cache.getStatus();
        LocalDateTime updatedAt = cache.getUpdatedAt();
        LocalDateTime now = LocalDateTime.now();

        boolean canStart = status == PosterStatus.PENDING;

        boolean canRetryFailed =
                status == PosterStatus.FAILED
                        && updatedAt != null
                        && updatedAt.isBefore(
                        now.minusHours(FAILED_RETRY_HOURS)
                );

        boolean canRecoverProcessing =
                status == PosterStatus.PROCESSING
                        && (
                        updatedAt == null
                                || updatedAt.isBefore(
                                now.minusMinutes(
                                        PROCESSING_TIMEOUT_MINUTES
                                )
                        )
                );

        if (!canStart && !canRetryFailed && !canRecoverProcessing) {
            return false;
        }

        cache.markAsProcessing();
        return true;
    }

    @Transactional
    public void updatePoster(String movieCode, String posterUrl) {

        posterCacheRepository.findById(movieCode)
                .ifPresent(cache -> cache.updatePoster(posterUrl));
    }

    @Transactional
    public void markAsFailed(String movieCode) {

        posterCacheRepository.findById(movieCode)
                .ifPresent(MoviePosterCache::markAsFailed);
    }

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