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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoviePosterCacheService {

    private final MoviePosterCacheRepository posterCacheRepository;
    private final PosterFileStorageService posterFileStorageService;

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

    // 파일이 유실된 포스터 캐시를 재수집 대기열로 되돌린다.
    @Transactional
    public void resetMissingPoster(String movieCode, String title, String releaseDate) {
        if (!posterCacheRepository.existsById(movieCode)) {
            createCache(movieCode, title, releaseDate);
        }
        posterCacheRepository.resetMissingPoster(movieCode, LocalDateTime.now());
    }

    @Transactional
    public String tryMarkAsProcessing(String movieCode) {
        LocalDateTime now = LocalDateTime.now();
        String claimToken = UUID.randomUUID().toString();
        int claimed = posterCacheRepository.tryClaim(
                movieCode,
                claimToken,
                now,
                now.minusHours(FAILED_RETRY_HOURS),
                now.minusMinutes(PROCESSING_TIMEOUT_MINUTES)
        );
        return claimed == 1 ? claimToken : null;
    }

    @Transactional
    public void releaseProcessingClaim(String movieCode, String claimToken) {
        if (claimToken != null) {
            posterCacheRepository.releaseClaim(movieCode, claimToken, LocalDateTime.now());
        }
    }

    @Transactional
    public void markAsFailed(String movieCode, String claimToken) {
        if (claimToken != null) {
            posterCacheRepository.failClaim(movieCode, claimToken, LocalDateTime.now());
        }
    }

    private boolean isPosterFileAvailable(String posterUrl) {
        if (posterUrl == null || posterUrl.isBlank()) {
            return false;
        }
        return !posterUrl.startsWith("/posters/")
                || posterFileStorageService.exists(posterUrl);
    }

    public List<MoviePosterStatusResponse> findPosterStatuses(Collection<String> movieCodes) {
        if (movieCodes == null || movieCodes.isEmpty()) {
            return List.of();
        }

        return posterCacheRepository.findAllByMovieCodeIn(movieCodes).stream()
                .map(cache -> new MoviePosterStatusResponse(
                        cache.getMovieCode(),
                        cache.getStatus(),
                        isPosterFileAvailable(cache.getPosterUrl()) ? cache.getPosterUrl() : ""
                ))
                .toList();
    }

    public Optional<MoviePosterStatusResponse> findPosterStatus(String movieCode) {
        return posterCacheRepository.findById(movieCode)
                .map(cache -> new MoviePosterStatusResponse(
                        cache.getMovieCode(),
                        cache.getStatus(),
                        isPosterFileAvailable(cache.getPosterUrl()) ? cache.getPosterUrl() : ""
                ));
    }
}
