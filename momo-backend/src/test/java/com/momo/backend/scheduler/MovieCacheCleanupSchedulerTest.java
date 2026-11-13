package com.momo.backend.scheduler;

import com.momo.backend.repository.MovieCacheRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MovieCacheCleanupSchedulerTest {

    @Test
    void retainsExpiredCacheForSevenDaysForUpstreamOutageFallback() {
        MovieCacheRepository repository = mock(MovieCacheRepository.class);
        MovieCacheCleanupScheduler scheduler = new MovieCacheCleanupScheduler(repository);
        LocalDateTime beforeRun = LocalDateTime.now();

        scheduler.deleteExpiredCaches();

        ArgumentCaptor<LocalDateTime> cutoffCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteExpired(cutoffCaptor.capture());

        LocalDateTime cutoff = cutoffCaptor.getValue();
        assertTrue(!cutoff.isAfter(beforeRun.minusDays(7).plusSeconds(1)));
        assertTrue(!cutoff.isBefore(beforeRun.minusDays(7).minusSeconds(1)));
    }
}
