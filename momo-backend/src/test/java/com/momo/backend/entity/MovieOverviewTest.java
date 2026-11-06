package com.momo.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MovieOverviewTest {

    @Test
    void overviewIsTrimmedAndStoredOnlyOnce() {
        Movie movie = new Movie("00000001", "테스트 영화", "", "20200101",
                "", "", "", "", "");

        movie.updateOverview("  영화 줄거리  ");
        movie.updateOverview("다른 줄거리");

        assertEquals("영화 줄거리", movie.getOverview());
    }

    @Test
    void blankOverviewIsIgnored() {
        Movie movie = new Movie("00000001", "테스트 영화", "", "20200101",
                "", "", "", "", "");

        movie.updateOverview(" ");
        assertNull(movie.getOverview());
    }

    @Test
    void overviewLookupIsThrottledForSevenDays() {
        Movie movie = new Movie("00000001", "테스트 영화", "", "20200101",
                "", "", "", "", "");
        LocalDateTime now = LocalDateTime.now();

        assertTrue(movie.canAttemptOverview(now));

        movie.markOverviewAttempted(now);

        assertFalse(movie.canAttemptOverview(now.plusDays(6)));
        assertTrue(movie.canAttemptOverview(now.plusDays(8)));
    }
}
