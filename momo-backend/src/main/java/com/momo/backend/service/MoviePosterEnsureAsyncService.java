package com.momo.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 리뷰·커뮤니티 목록에서 누락된 영화의 포스터 수집을 요청 스레드와 분리한다.
 * MovieService가 Movie 행이 없는 경우 KOBIS 상세 조회를 수행하므로
 * 여러 영화 코드를 순차 처리하면 외부 API 장애 시 화면 응답이 길어질 수 있다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePosterEnsureAsyncService {

    private final MovieService movieService;

    @Async("posterEnsureTaskExecutor")
    public void ensurePoster(String movieCode) {
        try {
            movieService.getMoviePoster(movieCode);
        } catch (Exception exception) {
            // 한 영화의 외부 API 실패가 다른 영화의 포스터 수집 요청에 영향을 주지 않게 한다.
            log.warn("비동기 포스터 수집 요청 실패. movieCode={}", movieCode, exception);
        }
    }
}
