package com.momo.backend.scheduler;

import com.momo.backend.service.MovieCatalogSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "momo.catalog.sync.enabled", havingValue = "true", matchIfMissing = true)
public class MovieCatalogSyncScheduler {

    private final MovieCatalogSyncService catalogSyncService;

    // 한 번에 100개씩만 수집해 외부 API 호출량을 제한하고, 서버 재시작 후에도 다음 페이지부터 이어간다.
    @Scheduled(fixedDelayString = "${momo.catalog.sync.delay-ms:5000}")
    public void collectNextCatalogPage() {
        catalogSyncService.syncNextPage();
    }
}
