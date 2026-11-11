package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import com.momo.backend.client.TmdbApiClient;
import com.momo.backend.repository.MovieCacheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class PersonFilmographyMatchingTest {

    @Test
    void connectsMovieWhenProductionYearMatchesEvenIfKoreanReleaseDateDiffers() {
        long tmdbId = 900001L;
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        KobisApiClient kobisApiClient = mock(KobisApiClient.class);
        MoviePersistenceService moviePersistenceService = mock(MoviePersistenceService.class);
        MoviePosterPersistenceService posterPersistenceService =
                mock(MoviePosterPersistenceService.class);

        when(tmdbApiClient.getMovieDetail(tmdbId)).thenReturn("""
                {
                  "title": "한국 제목",
                  "original_title": "Original Title",
                  "release_date": "2023-12-20",
                  "release_dates": {
                    "results": [{
                      "iso_3166_1": "KR",
                      "release_dates": [
                        {
                          "release_date": "2023-12-20T00:00:00.000Z",
                          "type": 1
                        },
                        {
                          "release_date": "2024-12-25T00:00:00.000Z",
                          "type": 3
                        }
                      ]
                    }]
                  },
                  "credits": {
                    "crew": [{"job": "Director", "name": "TMDb 감독 표기"}]
                  },
                  "poster_path": "/film-poster.jpg"
                }
                """);
        when(tmdbApiClient.getMovieAlternativeTitles(tmdbId))
                .thenReturn("{\"titles\":[]}");

        when(kobisApiClient.searchMovies("한국 제목", 1, 100)).thenReturn("""
                {
                  "movieListResult": {
                    "totCnt": 1,
                    "movieList": [{
                      "movieCd": "20241234",
                      "movieNm": "한국 제목",
                      "movieNmEn": "Original Title",
                      "openDt": "20250110",
                      "prdtYear": "2024",
                      "genreAlt": "드라마",
                      "directors": [{"peopleNm": "KOBIS 감독 표기"}]
                    }]
                  }
                }
                """);
        when(kobisApiClient.searchMovies("Original Title", 1, 100))
                .thenReturn("{\"movieListResult\":{\"totCnt\":0,\"movieList\":[]}}");

        PersonService service = createService(
                tmdbApiClient, kobisApiClient,
                moviePersistenceService, posterPersistenceService);

        assertEquals("20241234", service.resolveFilmographyMovie(tmdbId));

        verify(moviePersistenceService).saveSearchMovies(argThat(movies ->
                movies.size() == 1
                        && "20241234".equals(movies.get(0).movieCode())
                        && "한국 제목".equals(movies.get(0).title())
                        && "20250110".equals(movies.get(0).releaseDate())
                        && "드라마".equals(movies.get(0).genre())
        ));
        verify(posterPersistenceService).saveFilmographyPosterIfMissing(
                "20241234",
                "https://image.tmdb.org/t/p/w500/film-poster.jpg"
        );
    }

    @Test
    void connectsWithMatchingDirectorAndYearWhenKobisTitleUsesAnotherLocalName() {
        long tmdbId = 900002L;
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        KobisApiClient kobisApiClient = mock(KobisApiClient.class);
        MoviePersistenceService moviePersistenceService = mock(MoviePersistenceService.class);
        MoviePosterPersistenceService posterPersistenceService =
                mock(MoviePosterPersistenceService.class);

        when(tmdbApiClient.getMovieDetail(tmdbId)).thenReturn("""
                {
                  "title": "TMDb Localized Title",
                  "original_title": "Original Title",
                  "release_date": "2024-06-01",
                  "release_dates": {
                    "results": [{
                      "iso_3166_1": "KR",
                      "release_dates": [{
                        "release_date": "2024-06-01T00:00:00.000Z",
                        "type": 3
                      }]
                    }]
                  },
                  "credits": {
                    "crew": [{"job": "Director", "name": "Akiyuki Shinbo"}]
                  }
                }
                """);
        when(tmdbApiClient.getMovieAlternativeTitles(tmdbId))
                .thenReturn("{\"titles\":[]}");

        when(kobisApiClient.searchMovies("TMDb Localized Title", 1, 100)).thenReturn("""
                {
                  "movieListResult": {
                    "totCnt": 1,
                    "movieList": [{
                      "movieCd": "20245678",
                      "movieNm": "국내 배급 표기 제목",
                      "movieNmEn": "Another English Listing",
                      "openDt": "20240601",
                      "prdtYear": "2024",
                      "directors": [{"peopleNm": "Akiyuki Shinbo"}]
                    }]
                  }
                }
                """);
        when(kobisApiClient.searchMovies("Original Title", 1, 100))
                .thenReturn("{\"movieListResult\":{\"totCnt\":0,\"movieList\":[]}}");

        PersonService service = createService(
                tmdbApiClient, kobisApiClient,
                moviePersistenceService, posterPersistenceService);

        assertEquals("20245678", service.resolveFilmographyMovie(tmdbId));
        verify(moviePersistenceService).saveSearchMovies(argThat(movies ->
                movies.size() == 1
                        && "20245678".equals(movies.get(0).movieCode())
        ));
    }

    @Test
    void doesNotChooseArbitrarilyWhenTwoCandidatesHaveTheSameScore() {
        long tmdbId = 900003L;
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        KobisApiClient kobisApiClient = mock(KobisApiClient.class);

        when(tmdbApiClient.getMovieDetail(tmdbId)).thenReturn("""
                {
                  "title": "동명 영화",
                  "original_title": "Same Title",
                  "release_date": "2024-06-01",
                  "release_dates": {
                    "results": [{
                      "iso_3166_1": "KR",
                      "release_dates": [{
                        "release_date": "2024-06-01T00:00:00.000Z",
                        "type": 3
                      }]
                    }]
                  },
                  "credits": {"crew": []}
                }
                """);
        when(tmdbApiClient.getMovieAlternativeTitles(tmdbId))
                .thenReturn("{\"titles\":[]}");
        when(kobisApiClient.searchMovies("동명 영화", 1, 100)).thenReturn("""
                {
                  "movieListResult": {
                    "totCnt": 2,
                    "movieList": [
                      {
                        "movieCd": "20240001",
                        "movieNm": "동명 영화",
                        "movieNmEn": "Same Title",
                        "openDt": "20240601",
                        "prdtYear": "2024",
                        "directors": []
                      },
                      {
                        "movieCd": "20240002",
                        "movieNm": "동명 영화",
                        "movieNmEn": "Same Title",
                        "openDt": "20240601",
                        "prdtYear": "2024",
                        "directors": []
                      }
                    ]
                  }
                }
                """);
        when(kobisApiClient.searchMovies("Same Title", 1, 100))
                .thenReturn("{\"movieListResult\":{\"totCnt\":0,\"movieList\":[]}}");

        PersonService service = createService(
                tmdbApiClient, kobisApiClient,
                mock(MoviePersistenceService.class),
                mock(MoviePosterPersistenceService.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.resolveFilmographyMovie(tmdbId)
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    private PersonService createService(
            TmdbApiClient tmdbApiClient,
            KobisApiClient kobisApiClient,
            MoviePersistenceService moviePersistenceService,
            MoviePosterPersistenceService posterPersistenceService
    ) {
        return new PersonService(
                tmdbApiClient,
                kobisApiClient,
                mock(MovieCacheRepository.class),
                moviePersistenceService,
                posterPersistenceService,
                new ObjectMapper()
        );
    }
}
