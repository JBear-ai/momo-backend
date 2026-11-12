package com.momo.backend.service;

import com.momo.backend.client.KmdbApiClient;
import com.momo.backend.client.TmdbApiClient;
import com.github.benmanes.caffeine.cache.Caffeine;
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

        when(tmdbApiClient.searchMovie("한국 제목", "ko-KR"))
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

        when(tmdbApiClient.searchMovie("한국 제목", "ko-KR"))
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

    @Test
    void findsMadokaMovieDetailsWithEnglishTmdbSearchWhenKobisReleaseDateIsMissing() {
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(tmdbApiClient.searchMovie(
                "극장판 마법소녀 마도카☆마기카〈발푸르기스의 회천〉", "ko-KR"))
                .thenReturn("{\"results\":[]}");
        when(tmdbApiClient.searchMovie(
                "Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-", "en-US"))
                .thenReturn("""
                        {"results":[{
                          "id":822653,
                          "title":"Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-",
                          "original_title":"Gekijouban Mahou Shoujo Madoka Magica: Walpurgis no Kaiten",
                          "release_date":"2026-08-28",
                          "poster_path":"/madoka.jpg"
                        }]}
                        """);
        when(tmdbApiClient.getMovieDetail(822653L))
                .thenReturn("""
                        {
                          "runtime":119,
                          "genres":[{"name":"Animation"}],
                          "release_dates":{"results":[{
                            "iso_3166_1":"KR",
                            "release_dates":[{
                              "release_date":"2026-11-04T00:00:00.000Z",
                              "type":3
                            }]
                          }]},
                          "credits":{
                            "crew":[
                              {"job":"Director","name":"Akiyuki Shinbo"},
                              {"job":"Director","name":"Yukihiro Miyamoto"}
                            ],
                            "cast":[
                              {"name":"Aoi Yuki"},
                              {"name":"Chiwa Saito"}
                            ]
                          }
                        }
                        """);

        TmdbService service = new TmdbService(tmdbApiClient, objectMapper);

        com.momo.backend.entity.MovieSupplement supplement =
                service.findMovieSupplement(
                        "극장판 마법소녀 마도카☆마기카〈발푸르기스의 회천〉",
                        "Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-",
                        "",
                        ""
                ).orElseThrow();

        assertEquals("20261104", supplement.releaseDate());
        assertEquals("119", supplement.showTime());
        assertTrue(supplement.genre().contains("Animation"));
        assertTrue(supplement.director().contains("Akiyuki Shinbo"));
        assertTrue(supplement.director().contains("Yukihiro Miyamoto"));
        assertEquals(
                java.util.List.of("Aoi Yuki", "Chiwa Saito"),
                supplement.actors()
        );

        verify(tmdbApiClient).searchMovie(
                "Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-", "en-US");
        verify(tmdbApiClient).getMovieDetail(822653L);

        String posterUrl = service.findMoviePosterUrl(
                "극장판 마법소녀 마도카☆마기카〈발푸르기스의 회천〉",
                "Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-",
                "",
                ""
        );

        assertEquals(
                "https://image.tmdb.org/t/p/w500/madoka.jpg",
                posterUrl
        );
    }

    @Test
    void retriesMissingMovieDetailsAfterSixHours() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.momo.backend.entity.Movie movie = new com.momo.backend.entity.Movie(
                "20267412", "테스트 영화", "Test Movie",
                "", "119", "애니메이션", "", "", "15세이상관람가"
        );

        movie.markSupplementAttempted(now.minusHours(7));
        movie.markKobisDetailAttempted(now.minusHours(7));

        assertTrue(movie.canAttemptSupplement(now));
        assertTrue(movie.canAttemptKobisDetail(now));
    }


    @Test
    void supplementsMissingReleaseDateAndDirectorFromKobisMovieList() {
        com.momo.backend.client.KobisApiClient kobisApiClient =
                mock(com.momo.backend.client.KobisApiClient.class);
        KmdbService kmdbService = mock(KmdbService.class);
        TmdbService tmdbService = mock(TmdbService.class);
        com.momo.backend.repository.MovieRepository movieRepository =
                mock(com.momo.backend.repository.MovieRepository.class);

        com.momo.backend.entity.Movie movie = new com.momo.backend.entity.Movie(
                "20267412",
                "극장판 마법소녀 마도카☆마기카〈발푸르기스의 회천〉",
                "Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-",
                "",
                "119",
                "애니메이션",
                "",
                "",
                "15세이상관람가"
        );

        when(movieRepository.findById("20267412"))
                .thenReturn(java.util.Optional.of(movie));
        when(kobisApiClient.getMovieDetail("20267412"))
                .thenReturn("""
                        {"movieInfoResult":{"movieInfo":{
                          "movieCd":"20267412",
                          "movieNm":"극장판 마법소녀 마도카☆마기카〈발푸르기스의 회천〉",
                          "movieNmEn":"Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-",
                          "openDt":"",
                          "showTm":"119",
                          "directors":[],
                          "genres":[{"genreNm":"애니메이션"}],
                          "actors":[],
                          "audits":[{"watchGradeNm":"15세이상관람가"}]
                        }}}
                        """);
        when(kobisApiClient.searchMovies(
                "극장판 마법소녀 마도카☆마기카〈발푸르기스의 회천〉", 1, 100))
                .thenReturn("""
                        {"movieListResult":{"movieList":[{
                          "movieCd":"20267412",
                          "movieNm":"극장판 마법소녀 마도카☆마기카〈발푸르기스의 회천〉",
                          "movieNmEn":"Puella Magi Madoka Magica the Movie -Walpurgisnacht: Rising-",
                          "openDt":"20261104",
                          "genreAlt":"애니메이션",
                          "directors":[{"peopleNm":"신보 아키유키"}]
                        }]}}
                        """);

        when(kmdbService.findMovieSupplement(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
        when(tmdbService.findMovieSupplement(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.of(
                        new com.momo.backend.entity.MovieSupplement(
                                "",
                                "",
                                "",
                                "",
                                "",
                                java.util.List.of("아오이 유키", "사이토 치와"),
                                "",
                                "TMDb"
                        )
                ));
        when(tmdbService.findMovieOverview(anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());

        MovieService service = new MovieService(
                mock(MoviePosterAsyncService.class),
                kobisApiClient,
                kmdbService,
                tmdbService,
                new ObjectMapper(),
                movieRepository,
                mock(com.momo.backend.repository.MovieCacheRepository.class),
                mock(MoviePosterCacheService.class),
                mock(MoviePersistenceService.class),
                mock(com.momo.backend.repository.MoviePosterCacheRepository.class),
                mock(com.momo.backend.repository.MovieExternalIdMappingRepository.class),
                mock(PosterFileStorageService.class),
                Caffeine.newBuilder().maximumSize(10).build()
        );

        com.momo.backend.dto.movie.MovieDetailResponse response =
                service.getMovieDetail("20267412");

        assertEquals("20261104", response.releaseDate());
        assertEquals("신보 아키유키", response.director());
        assertEquals(
                java.util.List.of("아오이 유키", "사이토 치와"),
                response.actors()
        );

        verify(kobisApiClient).searchMovies(
                "극장판 마법소녀 마도카☆마기카〈발푸르기스의 회천〉", 1, 100);
    }

}
