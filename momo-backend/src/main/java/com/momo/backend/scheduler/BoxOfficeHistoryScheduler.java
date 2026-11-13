package com.momo.backend.scheduler;

import com.momo.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 추천 요청에서 외부 KOBIS 16주 조회를 수행하지 않도록 백그라운드로 분리한다.
@Component
@ConditionalOnProperty(name = "momo.boxoffice.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class BoxOfficeHistoryScheduler {

    private final MovieService movieService;

    // 서버 시작 30초 후 최초 수집, 이후 10분마다 만료 캐시와 누락 주차를 보완한다.
    @Scheduled(
            initialDelayString = "${momo.boxoffice.history.initial-delay-ms:30000}",
            fixedDelayString = "${momo.boxoffice.history.refresh-delay-ms:600000}"
    )
    public void refreshWeeklyHistory() {
        try {
            movieService.refreshWeeklyBoxOfficeHistory();
        } catch (Exception e) {
            log.warn("주간 박스오피스 백그라운드 갱신 실패. 서버 실행은 계속합니다.", e);
        }
    }
}
