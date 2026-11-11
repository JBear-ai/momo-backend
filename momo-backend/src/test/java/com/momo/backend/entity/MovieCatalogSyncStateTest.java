package com.momo.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieCatalogSyncStateTest {

    @Test
    void continuesAcrossPagesAndFinishesAtReportedTotal() {
        MovieCatalogSyncState state = MovieCatalogSyncState.initial();
        LocalDateTime now = LocalDateTime.of(2026, 11, 10, 12, 0);

        state.completePage(1, 100, 250, now);
        assertEquals("INITIAL", state.getPhase());
        assertEquals(2, state.getNextPage());
        assertEquals(3, state.getTotalPages());

        state.completePage(2, 100, 250, now.plusSeconds(5));
        assertEquals(3, state.getNextPage());

        state.completePage(3, 100, 250, now.plusSeconds(10));
        assertEquals("IDLE", state.getPhase());
        assertEquals(1, state.getNextPage());
        assertEquals(now.plusSeconds(10), state.getLastCompletedAt());
    }

    @Test
    void retriesAfterTemporaryFailureAndStartsRecentSyncNextDay() {
        MovieCatalogSyncState state = MovieCatalogSyncState.initial();
        LocalDateTime now = LocalDateTime.of(2026, 11, 10, 12, 0);

        state.recordFailure("temporary failure", now);
        assertFalse(state.canRetry(now.plusSeconds(30)));
        assertTrue(state.canRetry(now.plusMinutes(1)));

        state.completePage(1, 100, 0, now.plusMinutes(2));
        assertTrue(state.needsRecentSync(now.plusDays(1)));
        state.startRecentSync(2025, 2026, now.plusDays(1));
        assertEquals("RECENT", state.getPhase());
        assertEquals(2025, state.getStartYear());
        assertEquals(2026, state.getEndYear());
    }
}
