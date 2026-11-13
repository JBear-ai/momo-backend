package com.momo.backend.scheduler;

import com.momo.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 추천용 일별·주간 이력은 사용자 요청과 분리하여 백그라운드에서 수집한다.
@Component
@ConditionalOnProperty(name = "momo.boxoffice.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class BoxOfficeHistoryScheduler {

    private final MovieService movieService;

    // 서버 시작 후 바로 갱신을 시작하되 추천 요청은 DB에 있는 후보를 즉시 조회한다.
    // 5분마다 확인하고 실제 재수집 간격(성공 45분·실패 5분)은 서비스가 제한한다.
    @Scheduled(
            initialDelayString = "${momo.boxoffice.history.daily-initial-delay-ms:0}",
            fixedDelayString = "${momo.boxoffice.history.daily-refresh-delay-ms:300000}"
    )
    public void refreshRecentHistory() {
        try {
            movieService.refreshRecentBoxOfficeHistory();
        } catch (Exception e) {
            log.warn("최근 박스오피스 백그라운드 갱신 실패. 서버 실행은 계속합니다.", e);
        }
    }

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
