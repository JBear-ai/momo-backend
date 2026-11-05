package com.momo.backend.scheduler;

import com.momo.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
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

        movieService.getDailyBoxOffice(targetDate);

        System.out.println("박스오피스 자동 조회 완료: " + targetDate);
    }

    // 서버 시작 시 전날 박스오피스 확인
    @EventListener(ApplicationReadyEvent.class)
    public void checkBoxOfficeOnStartup() {

        String targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        movieService.getDailyBoxOffice(targetDate);

        System.out.println("서버 시작 시 박스오피스 확인 완료: " + targetDate);
    }
    // 어떻게 작동할까요?
    // 스프링 서버 시작 완료 → 전날 날짜 계산 → mysql에 해당 날짜 데이터가 있는가?
    // 있으면 재사용 없으면 KOBIS.KMDB 조회 후 저장
}