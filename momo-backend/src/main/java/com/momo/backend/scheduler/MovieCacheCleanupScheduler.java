
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

    // API 장애 시 최신 응답을 임시로 제공할 수 있도록 TTL 만료 후 7일간 보관한다.
    private static final long STALE_CACHE_RETENTION_DAYS = 7;

    private final MovieCacheRepository movieCacheRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredCaches() {
        LocalDateTime retentionCutoff = LocalDateTime.now()
                .minusDays(STALE_CACHE_RETENTION_DAYS);
        int deleted = movieCacheRepository.deleteExpired(retentionCutoff);

        if (deleted > 0) {
            log.info("만료 후 {}일이 지난 영화 API 캐시 {}건 삭제",
                    STALE_CACHE_RETENTION_DAYS, deleted);
        }
    }
}
