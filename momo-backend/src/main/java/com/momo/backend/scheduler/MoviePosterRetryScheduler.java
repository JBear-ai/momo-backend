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

    // 신규 수집으로 스레드 풀에 등록되지 못한 포스터 작업을 작은 배치로 다시 등록한다.
    @Scheduled(fixedDelayString = "${momo.poster.retry.delay-ms:15000}")
    public void retryPendingPosterDownloads() {
        List<MoviePosterCache> candidates = new ArrayList<>();
        candidates.addAll(posterCacheRepository.findTop10ByStatusOrderByUpdatedAtAsc(PosterStatus.PENDING));
        candidates.addAll(posterCacheRepository.findTop10ByStatusOrderByUpdatedAtAsc(PosterStatus.FAILED));
        candidates.addAll(posterCacheRepository.findTop10ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                PosterStatus.PROCESSING, LocalDateTime.now().minusMinutes(10)));

        int submitted = 0;
        for (MoviePosterCache candidate : candidates) {
            if (submitted >= 3) {
                break;
            }

            Movie movie = movieRepository.findById(candidate.getMovieCode()).orElse(null);
            if (movie == null || (movie.getPosterUrl() != null && !movie.getPosterUrl().isBlank())) {
                continue;
            }

            String claimToken = posterCacheService.tryMarkAsProcessing(candidate.getMovieCode());
            if (claimToken == null) {
                continue;
            }

            try {
                posterAsyncService.searchPosterAsync(
                        candidate.getMovieCode(),
                        candidate.getTitle(),
                        candidate.getReleaseDate(),
                        claimToken
                );
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
