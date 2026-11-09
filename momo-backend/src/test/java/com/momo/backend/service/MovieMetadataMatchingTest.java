package com.momo.backend.service;

import com.momo.backend.client.KmdbApiClient;
import com.momo.backend.client.TmdbApiClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovieMetadataMatchingTest {

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
                "테스트 영화", "Test Movie", "20260101", "홍길동",
                "테스트 영화", "Test Movie", "2025-12-31", "홍길동"
        ));
    }

    @Test
    void usesEnglishTitleWhenCandidateReleaseYearIsMissing() {
        assertTrue(MovieMatchVerifier.isSameMovie(
                "한국 제목", "English Original Title", "20260101", "",
                "다른 표기 제목", "English Original Title", "", ""
        ));
    }

    @Test
    void matchesOneDirectorFromExternalDirectorList() {
        assertTrue(MovieMatchVerifier.isSameMovie(
                "동명 영화", "", "20260101", "홍길동",
                "동명 영화", "", "", "홍길동,김철수"
        ));
    }

    @Test
    void doesNotAcceptTitleOnlyWhenReleaseYearAndDirectorAreMissing() {
        assertFalse(MovieMatchVerifier.isSameMovie(
                "동명 영화", "", "", "",
                "동명 영화", "", "", ""
        ));
    }

    @Test
    void searchesEnglishTitleInKmdbWhenKoreanSearchHasNoResult() {
        KmdbApiClient kmdbApiClient = mock(KmdbApiClient.class);
        TmdbService tmdbService = mock(TmdbService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(kmdbApiClient.searchMovie("한국 제목"))
                .thenReturn("{\"Data\":[]}");
        when(kmdbApiClient.searchMovie("English Title"))
                .thenReturn("""
                        {"Data":[{"Result":[{
                          "title":"한국 제목",
                          "titleEng":"English Title",
                          "repRlsDate":"20261109",
                          "prodYear":"2026",
                          "directors":{"director":[{"directorNm":"홍길동"}]},
                          "posters":"https://example.com/poster.jpg"
                        }]}]}
                        """);

        KmdbService service = new KmdbService(
                kmdbApiClient, tmdbService, objectMapper);

        String posterUrl = service.findPosterUrl(
                "20260001", "한국 제목", "English Title", "20261109", "홍길동");

        assertEquals("https://example.com/poster.jpg", posterUrl);
        verify(kmdbApiClient).searchMovie("한국 제목");
        verify(kmdbApiClient).searchMovie("English Title");
        verifyNoInteractions(tmdbService);
    }

    @Test
    void fallsBackToTmdbWhenKmdbHasNoPoster() {
        KmdbApiClient kmdbApiClient = mock(KmdbApiClient.class);
        TmdbService tmdbService = mock(TmdbService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(kmdbApiClient.searchMovie("한국 제목"))
                .thenReturn("{\"Data\":[]}");
        when(kmdbApiClient.searchMovie("English Title"))
                .thenReturn("{\"Data\":[]}");
        when(tmdbService.findMoviePosterUrl(
                "한국 제목", "English Title", "20261109", "홍길동"))
                .thenReturn("https://example.com/tmdb-poster.jpg");

        KmdbService service = new KmdbService(
                kmdbApiClient, tmdbService, objectMapper);

        String posterUrl = service.findPosterUrl(
                "20260002", "한국 제목", "English Title", "20261109", "홍길동");

        assertEquals("https://example.com/tmdb-poster.jpg", posterUrl);
        verify(tmdbService).findMoviePosterUrl(
                "한국 제목", "English Title", "20261109", "홍길동");
    }

    @Test
    void acceptsTmdbCandidateWhenKoreanReleaseYearMatchesKobis() {
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(tmdbApiClient.searchMovie("한국 제목"))
                .thenReturn("""
                        {"results":[{
                          "id":822653,
                          "title":"Walpurgisnacht: Rising",
                          "original_title":"English Original Title",
                          "release_date":"2025-12-31",
                          "poster_path":"/poster.jpg"
                        }]}
                        """);
        when(tmdbApiClient.getMovieDetail(822653L))
                .thenReturn("""
                        {
                          "release_dates":{"results":[{
                            "iso_3166_1":"KR",
                            "release_dates":[{
                              "release_date":"2026-01-01T00:00:00.000Z",
                              "type":3
                            }]
                          }]},
                          "credits":{"crew":[]}
                        }
                        """);

        TmdbService service = new TmdbService(tmdbApiClient, objectMapper);

        String posterUrl = service.findMoviePosterUrl(
                "한국 제목", "English Original Title", "20260101", "");

        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", posterUrl);
        verify(tmdbApiClient).getMovieDetail(822653L);
    }

    @Test
    void usesDirectorWhenTmdbCandidateHasNoReleaseYear() {
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(tmdbApiClient.searchMovie("한국 제목"))
                .thenReturn("""
                        {"results":[{
                          "id":822654,
                          "title":"한국 제목",
                          "original_title":"한국 제목",
                          "release_date":"",
                          "poster_path":"/poster.jpg"
                        }]}
                        """);
        when(tmdbApiClient.getMovieDetail(822654L))
                .thenReturn("""
                        {
                          "release_dates":{"results":[]},
                          "credits":{"crew":[
                            {"job":"Director","name":"홍길동"}
                          ]}
                        }
                        """);

        TmdbService service = new TmdbService(tmdbApiClient, objectMapper);

        String posterUrl = service.findMoviePosterUrl(
                "한국 제목", "", "20260101", "홍길동");

        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", posterUrl);
        verify(tmdbApiClient).getMovieDetail(822654L);
    }
}
