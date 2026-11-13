package com.momo.backend.service;

import com.momo.backend.dto.movie.MoviePosterStatusResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoviePosterCacheService {

    private final MoviePosterCacheRepository posterCacheRepository;
    private final MovieRepository movieRepository;
    private final PosterFileStorageService posterFileStorageService;

    private static final long FAILED_RETRY_MINUTES = 2;
    private static final long PROCESSING_TIMEOUT_MINUTES = 10;

    public Optional<MoviePosterCache> findCache(String movieCode) {
        return posterCacheRepository.findById(movieCode);
    }

    @Transactional
    public MoviePosterCache createCache(String movieCode, String title, String releaseDate) {
        return createCache(movieCode, title, releaseDate, 500);
    }

    @Transactional
    public MoviePosterCache createCache(String movieCode, String title, String releaseDate, int priority) {
        posterCacheRepository.insertIfAbsent(
                movieCode, title, releaseDate, Math.max(0, priority), LocalDateTime.now());
        return posterCacheRepository.findById(movieCode)
                .orElseThrow(() -> new IllegalStateException("포스터 캐시 생성 실패: " + movieCode));
    }

    // 파일이 유실된 포스터 캐시를 재수집 대기열로 되돌린다.
    @Transactional
    public void resetMissingPoster(String movieCode, String title, String releaseDate) {
        if (!posterCacheRepository.existsById(movieCode)) {
            createCache(movieCode, title, releaseDate);
        }
        LocalDateTime now = LocalDateTime.now();
        posterCacheRepository.resetMissingPoster(
                movieCode, now, now.minusMinutes(PROCESSING_TIMEOUT_MINUTES));
    }

    @Transactional
    public String tryMarkAsProcessing(String movieCode) {
        LocalDateTime now = LocalDateTime.now();
        String claimToken = UUID.randomUUID().toString();
        int claimed = posterCacheRepository.tryClaim(
                movieCode,
                claimToken,
                now,
                now.minusMinutes(FAILED_RETRY_MINUTES),
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
        return posterUrl.startsWith("/posters/")
                && posterFileStorageService.exists(posterUrl);
    }

    public List<MoviePosterStatusResponse> findPosterStatuses(Collection<String> movieCodes) {
        return findPosterStatuses(movieCodes, PosterStatus.FAILED);
    }

    // 비동기 ensure 요청 직후에는 캐시 행이 아직 없을 수 있으므로 호출 목적에 맞는 초기 상태를 반환한다.
    public List<MoviePosterStatusResponse> findPosterStatuses(
            Collection<String> movieCodes,
            PosterStatus missingCacheStatus) {
        if (movieCodes == null || movieCodes.isEmpty()) {
            return List.of();
        }

        List<String> codes = movieCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return List.of();
        }

        // 포스터 캐시가 없어도 movies 테이블에 정상 파일이 저장되어 있을 수 있다.
        Map<String, Movie> movies = new HashMap<>();
        movieRepository.findAllById(codes).forEach(movie ->
                movies.put(movie.getMovieCode(), movie));

        Map<String, MoviePosterCache> caches = new HashMap<>();
        posterCacheRepository.findAllByMovieCodeIn(codes).forEach(cache ->
                caches.put(cache.getMovieCode(), cache));

        // 요청한 모든 코드에 상태를 반환한다. 상태가 없는 작품은 포스터 미확인(FAILED)으로 처리한다.
        return codes.stream()
                .map(movieCode -> {
                    Movie movie = movies.get(movieCode);
                    String moviePosterUrl = movie == null ? "" : movie.getPosterUrl();

                    if (isPosterFileAvailable(moviePosterUrl)) {
                        return new MoviePosterStatusResponse(
                                movieCode, PosterStatus.SUCCESS, moviePosterUrl);
                    }

                    MoviePosterCache cache = caches.get(movieCode);
                    if (cache == null) {
                        return new MoviePosterStatusResponse(
                                movieCode, missingCacheStatus, "");
                    }

                    boolean cachePosterAvailable = isPosterFileAvailable(cache.getPosterUrl());
                    String posterUrl = cachePosterAvailable ? cache.getPosterUrl() : "";

                    // SUCCESS는 실제 로컬 파일까지 확인된 상태에만 사용한다.
                    // 과거 외부 URL이나 유실된 파일을 SUCCESS로 반환하면 프런트엔드가 재조회하지 않는다.
                    PosterStatus status = cache.getStatus() == PosterStatus.SUCCESS
                            && !cachePosterAvailable
                            ? PosterStatus.FAILED
                            : cache.getStatus();

                    return new MoviePosterStatusResponse(movieCode, status, posterUrl);
                })
                .toList();
    }

    public Optional<MoviePosterStatusResponse> findPosterStatus(String movieCode) {
        return posterCacheRepository.findById(movieCode)
                .map(cache -> {
                    boolean posterAvailable = isPosterFileAvailable(cache.getPosterUrl());
                    PosterStatus status = cache.getStatus() == PosterStatus.SUCCESS && !posterAvailable
                            ? PosterStatus.FAILED
                            : cache.getStatus();
                    return new MoviePosterStatusResponse(
                            cache.getMovieCode(),
                            status,
                            posterAvailable ? cache.getPosterUrl() : ""
                    );
                });
    }
}
