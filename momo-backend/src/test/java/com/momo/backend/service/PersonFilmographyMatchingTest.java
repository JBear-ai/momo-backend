package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import com.momo.backend.client.TmdbApiClient;
import com.momo.backend.entity.MovieExternalIdMapping;
import com.momo.backend.repository.MovieCacheRepository;
import com.momo.backend.repository.MovieExternalIdMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Optional;

class PersonFilmographyMatchingTest {

    @Test
    void connectsMovieWhenProductionYearMatchesEvenIfKoreanReleaseDateDiffers() {
        long tmdbId = 900001L;
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        KobisApiClient kobisApiClient = mock(KobisApiClient.class);
        MoviePersistenceService moviePersistenceService = mock(MoviePersistenceService.class);
        MoviePosterPersistenceService posterPersistenceService =
                mock(MoviePosterPersistenceService.class);
        MovieExternalIdMappingRepository mappingRepository =
                mock(MovieExternalIdMappingRepository.class);

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
                moviePersistenceService, posterPersistenceService, mappingRepository);

        assertEquals("20241234", service.resolveFilmographyMovie(tmdbId));
        verify(mappingRepository).saveAndFlush(argThat(mapping ->
                "TMDB".equals(mapping.getProvider())
                        && String.valueOf(tmdbId).equals(mapping.getExternalId())
                        && "20241234".equals(mapping.getMovieCode())
        ));

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


    @Test
    void checksLaterKobisPagesBeforeChoosingBetweenSameTitleRemakes() {
        long tmdbId = 900005L;
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
                  "credits": {
                    "crew": [{"job": "Director", "name": "정확한 감독"}]
                  }
                }
                """);
        when(tmdbApiClient.getMovieAlternativeTitles(tmdbId))
                .thenReturn("{\"titles\":[]}");

        StringBuilder firstPageMovies = new StringBuilder();
        for (int index = 1; index <= 100; index++) {
            if (index > 1) firstPageMovies.append(",");
            firstPageMovies.append("""
                    {
                      "movieCd": "%s",
                      "movieNm": "동명 영화",
                      "movieNmEn": "Same Title",
                      "openDt": "20240601",
                      "prdtYear": "2024",
                      "directors": []
                    }
                    """.formatted("2024" + String.format("%04d", index)));
        }

        when(kobisApiClient.searchMovies("동명 영화", 1, 100)).thenReturn("""
                {"movieListResult":{"totCnt":101,"movieList":[%s]}}
                """.formatted(firstPageMovies));
        when(kobisApiClient.searchMovies("동명 영화", 2, 100)).thenReturn("""
                {
                  "movieListResult": {
                    "totCnt": 101,
                    "movieList": [{
                      "movieCd": "20240101",
                      "movieNm": "동명 영화",
                      "movieNmEn": "Same Title",
                      "openDt": "20240601",
                      "prdtYear": "2024",
                      "directors": [{"peopleNm": "정확한 감독"}]
                    }]
                  }
                }
                """);
        when(kobisApiClient.searchMovies("Same Title", 1, 100))
                .thenReturn("{\"movieListResult\":{\"totCnt\":0,\"movieList\":[]}}");

        PersonService service = createService(
                tmdbApiClient,
                kobisApiClient,
                mock(MoviePersistenceService.class),
                mock(MoviePosterPersistenceService.class)
        );

        assertEquals("20240101", service.resolveFilmographyMovie(tmdbId));
        verify(kobisApiClient).searchMovies("동명 영화", 2, 100);
    }

    @Test
    void reusesPreviouslyVerifiedExternalMovieMappingWithoutCallingProviders() {
        long tmdbId = 900004L;
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        KobisApiClient kobisApiClient = mock(KobisApiClient.class);
        MovieExternalIdMappingRepository mappingRepository =
                mock(MovieExternalIdMappingRepository.class);

        when(mappingRepository.findByProviderAndExternalId("TMDB", String.valueOf(tmdbId)))
                .thenReturn(Optional.of(
                        new MovieExternalIdMapping("TMDB", String.valueOf(tmdbId), "20240004")
                ));

        PersonService service = new PersonService(
                tmdbApiClient,
                kobisApiClient,
                mock(MovieCacheRepository.class),
                mappingRepository,
                mock(MoviePersistenceService.class),
                mock(MoviePosterPersistenceService.class),
                new ObjectMapper()
        );

        assertEquals("20240004", service.resolveFilmographyMovie(tmdbId));
        verifyNoInteractions(tmdbApiClient, kobisApiClient);
    }

    private PersonService createService(
            TmdbApiClient tmdbApiClient,
            KobisApiClient kobisApiClient,
            MoviePersistenceService moviePersistenceService,
            MoviePosterPersistenceService posterPersistenceService
    ) {
        return createService(
                tmdbApiClient, kobisApiClient, moviePersistenceService,
                posterPersistenceService, mock(MovieExternalIdMappingRepository.class));
    }

    private PersonService createService(
            TmdbApiClient tmdbApiClient,
            KobisApiClient kobisApiClient,
            MoviePersistenceService moviePersistenceService,
            MoviePosterPersistenceService posterPersistenceService,
            MovieExternalIdMappingRepository mappingRepository
    ) {
        return new PersonService(
                tmdbApiClient,
                kobisApiClient,
                mock(MovieCacheRepository.class),
                mappingRepository,
                moviePersistenceService,
                posterPersistenceService,
                new ObjectMapper()
        );
    }

    @Test
    void cachesPersonDetailByTmdbIdAndRoleAcrossRepeatedRequests() throws Exception {
        long personId = 900006L;
        TmdbApiClient tmdbApiClient = mock(TmdbApiClient.class);
        when(tmdbApiClient.getPersonDetail(personId, "ko-KR")).thenReturn("""
                {
                  "id": 900006,
                  "name": "ID 캐시 인물",
                  "profile_path": "/person.jpg",
                  "known_for_department": "Acting",
                  "combined_credits": {
                    "cast": [{
                      "media_type": "movie",
                      "id": 700001,
                      "title": "ID 캐시 영화",
                      "release_date": "2024-01-01",
                      "character": "주연",
                      "poster_path": "/movie.jpg"
                    }]
                  }
                }
                """);

        MovieCacheRepository cacheRepository = mock(MovieCacheRepository.class);
        var cachedResponse = new com.momo.backend.dto.person.PersonDetailResponse(
                new com.momo.backend.dto.person.PersonDetailResponse.PersonInfo(
                        personId, "ID 캐시 인물", "https://image.tmdb.org/t/p/w500/person.jpg",
                        null, null, null, null, "Acting"),
                java.util.List.of(new com.momo.backend.dto.person.PersonDetailResponse.FilmographyItem(
                        700001L, "ID 캐시 영화", "2024-01-01",
                        "https://image.tmdb.org/t/p/w500/movie.jpg", "주연"))
        );
        var cachedEntity = new com.momo.backend.entity.MovieCache(
                "PERSON_DETAIL", "ID|actor|900006",
                new ObjectMapper().writeValueAsString(cachedResponse), 24);
        when(cacheRepository.findByCacheTypeAndCacheKey("PERSON_DETAIL", "ID|actor|900006"))
                .thenReturn(java.util.Optional.empty(), java.util.Optional.of(cachedEntity));

        PersonService service = new PersonService(
                tmdbApiClient,
                mock(KobisApiClient.class),
                cacheRepository,
                mock(MovieExternalIdMappingRepository.class),
                mock(MoviePersistenceService.class),
                mock(MoviePosterPersistenceService.class),
                new ObjectMapper()
        );

        var first = service.getPersonDetailById("actor", personId);
        var second = service.getPersonDetailById("actor", personId);

        assertEquals("ID 캐시 인물", first.personInfo().name());
        assertEquals("ID 캐시 인물", second.personInfo().name());
        assertEquals(1, second.filmography().size());
        assertEquals(700001L, second.filmography().get(0).tmdbId());
        verify(tmdbApiClient, times(1)).getPersonDetail(personId, "ko-KR");
        verify(cacheRepository).upsert(
                eq("PERSON_DETAIL"),
                eq("ID|actor|900006"),
                anyString(),
                any(),
                any()
        );
    }

}
