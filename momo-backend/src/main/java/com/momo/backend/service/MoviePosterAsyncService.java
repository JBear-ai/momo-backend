package com.momo.backend.service;

import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePosterAsyncService {

    private final KmdbService kmdbService;
    private final MoviePosterCacheService posterCacheService;
    private final MovieRepository movieRepository;

    @Async("posterTaskExecutor")
    public void searchPosterAsync(
            String movieCode,
            String title,
            String releaseDate
    ) {
        try {

            log.info("포스터 검색 시작: {}", movieCode);

            String posterUrl = kmdbService.findPosterUrl(
                    movieCode,
                    title,
                    releaseDate
            );

            if (posterUrl == null || posterUrl.isBlank()) {

                posterCacheService.markAsFailed(movieCode);

                log.warn("포스터 검색 결과 없음: {}", movieCode);
                return;
            }

            posterCacheService.updatePoster(
                    movieCode,
                    posterUrl
            );

            updateMoviePoster(movieCode, posterUrl);

            log.info("포스터 저장 성공: {}", movieCode);

        } catch (Exception e) {

            posterCacheService.markAsFailed(movieCode);

            log.error(
                    "포스터 검색 실패: {}",
                    movieCode,
                    e
            );
        }
    }

    @Transactional
    public void updateMoviePoster(
            String movieCode,
            String posterUrl
    ) {
        movieRepository.findById(movieCode)
                .ifPresent(movie -> movie.updatePosterUrl(posterUrl));
    }
}