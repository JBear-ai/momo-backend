package com.momo.backend.scheduler;

import com.momo.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Spring Boot가 BoxOfficeScheduler를 관리하도록 등록
@Component
@ConditionalOnProperty(name = "momo.boxoffice.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class BoxOfficeScheduler {

    private final MovieService movieService;

    // 매일 오전 3시에 전날 박스오피스 조회
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
//    테스트용 자동 조회 필요시 주석 on/off
//    @Scheduled(fixedDelay = 30000)
    public void updateDailyBoxOffice() {

        // 한국 시간 기준 어제 날짜
        String targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        refreshDailyBoxOffice(targetDate, "예약 작업");
    }

    // 서버 시작 시 전날 박스오피스 확인
    @EventListener(ApplicationReadyEvent.class)
    public void checkBoxOfficeOnStartup() {

        String targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        refreshDailyBoxOffice(targetDate, "서버 시작");
    }

    // 외부 KOBIS 장애가 애플리케이션 기동이나 예약 작업을 중단시키지 않도록 격리한다.
    private void refreshDailyBoxOffice(String targetDate, String trigger) {
        try {
            movieService.getDailyBoxOffice(targetDate);
            log.info("{} 박스오피스 조회 완료: {}", trigger, targetDate);
        } catch (Exception e) {
            log.warn("{} 박스오피스 조회 실패. 서버 실행을 계속합니다. targetDate: {}",
                    trigger, targetDate, e);
        }
    }
}