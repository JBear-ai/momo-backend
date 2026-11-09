package com.momo.backend.service;


import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePosterAsyncService {

    private final KmdbService kmdbService;
    private final MoviePosterCacheService posterCacheService;
    private final MoviePosterPersistenceService posterPersistenceService;
    private final MovieRepository movieRepository;

    @Async("posterTaskExecutor")
    public void searchPosterAsync(
            String movieCode,
            String title,
            String releaseDate
    ) {
        try {

            log.info("포스터 검색 시작: {}", movieCode);

            Movie movie = movieRepository.findById(movieCode).orElse(null);
            String titleEn = movie == null ? "" : movie.getTitleEn();
            String director = movie == null ? "" : movie.getDirector();

            String posterUrl = kmdbService.findPosterUrl(
                    movieCode,
                    title,
                    titleEn,
                    releaseDate,
                    director
            );

            if (posterUrl == null || posterUrl.isBlank()) {

                posterCacheService.markAsFailed(movieCode);

                log.warn("포스터 검색 결과 없음: {}", movieCode);
                return;
            }

            posterPersistenceService.saveSuccess(movieCode, posterUrl);

            log.info("포스터 저장 성공: {}", movieCode);

        } catch (Exception e) {

            // 외부 API 또는 일시적인 처리 오류는 검색 결과 없음과 다르다.
            // PROCESSING 상태를 유지하면 캐시의 10분 timeout 이후 다시 검색할 수 있다.
            log.error(
                    "포스터 검색 중 일시적인 오류 발생. 잠시 후 재시도합니다: {}",
                    movieCode,
                    e
            );
        }
    }

}
