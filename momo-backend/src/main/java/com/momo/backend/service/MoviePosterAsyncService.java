
package com.momo.backend.service;

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

    // 포스터 검색을 백그라운드에서 실행
    @Async("posterTaskExecutor")
    public void searchPosterAsync(
            String movieCode,
            String title,
            String releaseDate
    ) {

        try {

            log.info(
                    "포스터 검색 시작. movieCode: {}",
                    movieCode
            );

            // KMDb 및 TMDb에서 포스터 검색
            String posterUrl = kmdbService.findPosterUrl(
                    movieCode,
                    title,
                    releaseDate
            );

            // 포스터를 찾지 못한 경우
            if (posterUrl == null || posterUrl.isBlank()) {

                posterCacheService.markAsFailed(movieCode);

                log.warn(
                        "포스터 검색 결과 없음. movieCode: {}",
                        movieCode
                );

                return;
            }

            // 포스터 주소와 성공 상태 저장
            posterCacheService.updatePoster(
                    movieCode,
                    posterUrl
            );

            log.info(
                    "포스터 검색 성공. movieCode: {}",
                    movieCode
            );

        } catch (Exception e) {

            posterCacheService.markAsFailed(movieCode);

            log.error(
                    "포스터 백그라운드 검색 실패. movieCode: {}",
                    movieCode,
                    e
            );
        }
    }
}
