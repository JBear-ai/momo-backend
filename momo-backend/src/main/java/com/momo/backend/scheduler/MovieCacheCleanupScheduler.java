
package com.momo.backend.scheduler;

import com.momo.backend.repository.MovieCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieCacheCleanupScheduler {

    private final MovieCacheRepository movieCacheRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredCaches() {
        int deleted = movieCacheRepository.deleteExpired(LocalDateTime.now());

        if (deleted > 0) {
            log.info("만료된 영화 API 캐시 {}건 삭제", deleted);
        }
    }
}
