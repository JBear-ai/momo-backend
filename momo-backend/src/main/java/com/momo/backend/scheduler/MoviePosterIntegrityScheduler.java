package com.momo.backend.scheduler;

import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.service.MoviePosterCacheService;
import com.momo.backend.service.PosterFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DB에는 경로가 남아 있지만 실제 포스터 파일이 사라진 상황을 자동 복구한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "momo.poster.integrity.enabled", havingValue = "true", matchIfMissing = true)
public class MoviePosterIntegrityScheduler {

    private static final String LOCAL_POSTER_PREFIX = "/posters/";

    private final MovieRepository movieRepository;
    private final PosterFileStorageService posterFileStorageService;
    private final MoviePosterCacheService posterCacheService;

    // 5분마다 로컬 포스터 경로를 확인한다. 커서 방식이라 복구 중인 행 때문에 누락되지 않는다.
    @Scheduled(fixedDelayString = "${momo.poster.integrity.delay-ms:300000}", initialDelayString = "${momo.poster.integrity.initial-delay-ms:30000}")
    public void repairMissingPosterFiles() {
        String afterMovieCode = null;
        int repaired = 0;

        while (true) {
            List<Movie> batch = afterMovieCode == null
                    ? movieRepository.findTop100ByPosterUrlStartingWithOrderByMovieCodeAsc(
                            LOCAL_POSTER_PREFIX)
                    : movieRepository.findTop100ByPosterUrlStartingWithAndMovieCodeGreaterThanOrderByMovieCodeAsc(
                            LOCAL_POSTER_PREFIX, afterMovieCode);

            if (batch.isEmpty()) {
                break;
            }

            for (Movie movie : batch) {
                afterMovieCode = movie.getMovieCode();
                String posterPath = movie.getPosterUrl();

                if (posterFileStorageService.exists(posterPath)) {
                    continue;
                }

                // DB 경로 제거와 재수집 대기열 등록을 서비스의 단일 트랜잭션으로 처리한다.
                posterCacheService.repairMissingPosterFile(
                        movie.getMovieCode(),
                        movie.getTitle(),
                        movie.getReleaseDate()
                );
                repaired++;

                log.warn("누락된 포스터 파일 감지. 재수집 대기열로 이동. movieCode={}, path={}",
                        movie.getMovieCode(), posterPath);
            }

            if (batch.size() < 100) {
                break;
            }
        }

        if (repaired > 0) {
            log.info("포스터 파일 무결성 점검 완료. 자동 복구 등록={}건", repaired);
        }
    }
}
