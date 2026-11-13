package com.momo.backend.scheduler;

import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.service.MoviePosterAsyncService;
import com.momo.backend.service.MoviePosterCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "momo.poster.retry.enabled", havingValue = "true", matchIfMissing = true)
public class MoviePosterRetryScheduler {

    private final MoviePosterCacheRepository posterCacheRepository;
    private final MoviePosterCacheService posterCacheService;
    private final MoviePosterAsyncService posterAsyncService;
    private final MovieRepository movieRepository;

    private boolean isRetryReady(MoviePosterCache cache) {
        if (cache.getUpdatedAt() == null) {
            return true;
        }

        // 포스터 수집은 홈 화면 노출에 직접 영향을 주므로 재시도 간격을 최대 10분으로 제한한다.
        long retryDelayMinutes = switch (cache.getRetryCount()) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 5;
            default -> 10;
        };

        return !cache.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(retryDelayMinutes));
    }

    // 신규 수집으로 스레드 풀에 등록되지 못한 포스터 작업을 작은 배치로 다시 등록한다.
    @Scheduled(fixedDelayString = "${momo.poster.retry.delay-ms:15000}")
    public void retryPendingPosterDownloads() {
        List<MoviePosterCache> candidates = new ArrayList<>();
        candidates.addAll(posterCacheRepository.findTop50ByStatusOrderByPriorityAscUpdatedAtAsc(PosterStatus.PENDING));
        // 실패 횟수에 따라 1분 → 2분 → 5분 → 최대 10분 간격으로 재시도한다.
        posterCacheRepository.findTop50ByStatusOrderByPriorityAscUpdatedAtAsc(PosterStatus.FAILED)
                .stream()
                .filter(this::isRetryReady)
                .forEach(candidates::add);
        candidates.addAll(posterCacheRepository.findTop50ByStatusAndUpdatedAtBeforeOrderByPriorityAscUpdatedAtAsc(
                PosterStatus.PROCESSING, LocalDateTime.now().minusMinutes(10)));
        candidates.sort(java.util.Comparator.comparingInt((MoviePosterCache cache) -> cache.getPriority())
                .thenComparing(cache -> cache.getUpdatedAt(),
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));

        int submitted = 0;
        for (MoviePosterCache candidate : candidates) {
            if (submitted >= 5) {
                break;
            }

            Movie movie = movieRepository.findById(candidate.getMovieCode()).orElse(null);
            if (movie == null) {
                continue;
            }

            // FAILED는 Movie 테이블에 오래된 URL이 남아 있어도 재수집을 막지 않는다.
            // PENDING 작업만 기존 포스터 URL이 있으면 중복 수집을 피한다.
            if (candidate.getStatus() != PosterStatus.FAILED
                    && movie.getPosterUrl() != null
                    && !movie.getPosterUrl().isBlank()) {
                continue;
            }

            String claimToken = posterCacheService.tryMarkAsProcessing(candidate.getMovieCode());
            if (claimToken == null) {
                continue;
            }

            try {
                if (candidate.getPriority() == 0) {
                    posterAsyncService.searchBoxOfficePosterAsync(
                            candidate.getMovieCode(), candidate.getTitle(), candidate.getReleaseDate(), claimToken);
                } else {
                    posterAsyncService.searchPosterAsync(
                            candidate.getMovieCode(), candidate.getTitle(), candidate.getReleaseDate(), claimToken);
                }
                submitted++;
            } catch (RuntimeException exception) {
                posterCacheService.releaseProcessingClaim(candidate.getMovieCode(), claimToken);
                log.warn("포스터 재시도 작업 등록 실패. movieCode={}", candidate.getMovieCode(), exception);
            }
        }

        if (submitted > 0) {
            log.info("포스터 재시도 작업 등록 완료: {}건", submitted);
        }
    }
}
