package com.momo.backend.scheduler;

import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.service.PosterFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "momo.poster.migration.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MoviePosterMigrationScheduler {

    private final MovieRepository movieRepository;
    private final MoviePosterCacheRepository posterCacheRepository;
    private final PosterFileStorageService posterFileStorageService;

    // 기존 DB에 저장된 외부 포스터 주소도 5개씩 로컬 파일로 이전한다.
    @Scheduled(fixedDelayString = "${momo.poster.migration.delay-ms:60000}")
    public void migrateLegacyPosterUrls() {
        LocalDateTime now = LocalDateTime.now();
        List<Movie> movies = movieRepository.findMoviesNeedingPosterMigration(
                now.minusDays(1),
                PageRequest.of(0, 5)
        );

        for (Movie movie : movies) {
            String movieCode = movie.getMovieCode();
            String originalUrl = movie.getPosterUrl();

            // 실패하더라도 매 실행마다 같은 주소를 재요청하지 않도록 시도 시각부터 기록한다.
            movie.markPosterMigrationAttempted(now);
            movieRepository.save(movie);

            try {
                String localPath = posterFileStorageService.downloadAndSave(movieCode, originalUrl);
                movie.updatePosterUrl(localPath);
                movieRepository.save(movie);

                posterCacheRepository.findById(movieCode).ifPresent(cache -> {
                    cache.updatePosterUrl(localPath);
                    posterCacheRepository.save(cache);
                });

                log.info("기존 포스터 로컬 이전 완료. movieCode={}", movieCode);
            } catch (Exception exception) {
                log.warn("기존 포스터 로컬 이전 실패. movieCode={}. 하루 후 재시도합니다.",
                        movieCode, exception);
            }
        }
    }
}
