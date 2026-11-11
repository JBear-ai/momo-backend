package com.momo.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovieMatchVerifierTest {

    @Test
    void matchesKoreanTitleWhenReleaseYearIsTheSame() {
        assertTrue(MovieMatchVerifier.isSameMovie(
                "극장판 마법소녀 마도카☆마기카",
                "Puella Magi Madoka Magica",
                "20261109",
                "",
                "극장판 마법소녀 마도카☆마기카",
                "Puella Magi Madoka Magica",
                "2026-11-09",
                ""
        ));
    }

    @Test
    void matchesEnglishTitleAgainstOriginalTitle() {
        assertTrue(MovieMatchVerifier.isSameMovie(
                "발푸르기스의 회천",
                "Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-",
                "20261109",
                "",
                "Walpurgisnacht: Rising",
                "Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-",
                "2026-11-09",
                ""
        ));
    }

    @Test
    void rejectsSameTitleFromDifferentReleaseYear() {
        assertFalse(MovieMatchVerifier.isSameMovie(
                "테스트 영화",
                "Test Movie",
                "20260101",
                "",
                "테스트 영화",
                "Test Movie",
                "2025-12-31",
                ""
        ));
    }

    @Test
    void acceptsExactTitleWhenBothReleaseYearAndDirectorAreMissing() {
        assertTrue(MovieMatchVerifier.isSameMovie(
                "동명 영화",
                "",
                "",
                "",
                "동명 영화",
                "",
                "",
                ""
        ));
    }

    @Test
    void acceptsExactTitleWhenOnlyCandidateReleaseYearIsMissing() {
        assertTrue(MovieMatchVerifier.isSameMovie(
                "테스트 영화", "Test Movie", "20261109", "",
                "테스트 영화", "Test Movie", "", ""
        ));
    }

    @Test
    void canUseDirectorWhenReleaseYearIsUnavailable() {
        assertTrue(MovieMatchVerifier.isSameMovie(
                "동명 영화",
                "",
                "",
                "홍길동",
                "동명 영화",
                "",
                "",
                "홍길동"
        ));
    }

    @Test
    void rejectsDifferentDirectorWhenReleaseYearIsUnavailable() {
        assertFalse(MovieMatchVerifier.isSameMovie(
                "동명 영화",
                "",
                "",
                "홍길동",
                "동명 영화",
                "",
                "",
                "김철수"
        ));
    }
    @Test
    void prefersExactReleaseDateAndDirectorWhenRankingPosterCandidates() {
        int exactMatchScore = MovieMatchVerifier.matchScore(
                "테스트 영화",
                "Test Movie",
                "20261109",
                "홍길동",
                "테스트 영화",
                "Test Movie",
                "2026-11-09",
                "홍길동"
        );

        int weakerMatchScore = MovieMatchVerifier.matchScore(
                "테스트 영화",
                "Test Movie",
                "20261109",
                "홍길동",
                "테스트 영화",
                "Test Movie",
                "2026-11-08",
                ""
        );

        assertTrue(exactMatchScore > weakerMatchScore);
    }

    @Test
    void rejectsPosterCandidateWhenReleaseYearDoesNotMatch() {
        assertEquals(-1, MovieMatchVerifier.matchScore(
                "테스트 영화",
                "Test Movie",
                "20260101",
                "홍길동",
                "테스트 영화",
                "Test Movie",
                "2025-12-31",
                "홍길동"
        ));
    }

    @Test
    void rejectsSameTitleAndYearWhenKoreanDirectorConflicts() {
        assertFalse(MovieMatchVerifier.isSameMovie(
                "동명 영화",
                "Same Title",
                "20261109",
                "홍길동",
                "동명 영화",
                "Same Title",
                "2026-11-09",
                "김철수"
        ));
    }

}
