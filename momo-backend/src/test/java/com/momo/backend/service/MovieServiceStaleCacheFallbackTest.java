package com.momo.backend.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.momo.backend.client.KobisApiClient;
import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MovieCache;
import com.momo.backend.repository.MovieCacheRepository;
import com.momo.backend.repository.MovieExternalIdMappingRepository;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MovieServiceStaleCacheFallbackTest {

    private MovieService createService(
            KobisApiClient kobisApiClient,
            MovieCacheRepository cacheRepository,
            MovieRepository movieRepository,
            MoviePosterCacheRepository posterCacheRepository,
            MoviePersistenceService persistenceService,
            PosterFileStorageService posterStorageService
    ) {
        return new MovieService(
                mock(MoviePosterAsyncService.class),
                kobisApiClient,
                mock(KmdbService.class),
                mock(TmdbService.class),
                new ObjectMapper(),
                movieRepository,
                cacheRepository,
                mock(MoviePosterCacheService.class),
                persistenceService,
                posterCacheRepository,
                mock(MovieExternalIdMappingRepository.class),
                posterStorageService,
                Caffeine.newBuilder().maximumSize(20).build()
        );
    }

    private MovieCache expiredCache(String type, String key, String json) {
        MovieCache cache = new MovieCache(type, key, json, 1);
        ReflectionTestUtils.setField(
                cache, "expiresAt", LocalDateTime.now().minusMinutes(5));
        return cache;
    }

    @Test
    void servesRetainedSearchResultsWhenKobisIsUnavailable() {
        KobisApiClient kobis = mock(KobisApiClient.class);
        MovieCacheRepository cacheRepository = mock(MovieCacheRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MoviePosterCacheRepository posterCacheRepository =
                mock(MoviePosterCacheRepository.class);
        MovieCache stale = expiredCache(
                "SEARCH",
                "테스트 영화|1|20",
                """
                {
                  "content": [{
                    "movieCode": "20260001",
                    "title": "저장된 검색 결과",
                    "releaseDate": "20260101",
                    "genre": "드라마",
                    "director": "테스트 감독",
                    "posterUrl": ""
                  }],
                  "page": 1,
                  "size": 20,
                  "totalCount": 1,
                  "hasNext": false
                }
                """
        );

        when(cacheRepository.findByCacheTypeAndCacheKey("SEARCH", "테스트 영화|1|20"))
                .thenReturn(Optional.of(stale));
        when(kobis.searchMovies("테스트 영화", 1, 20))
                .thenThrow(new ResourceAccessException("KOBIS unavailable"));
        when(movieRepository.findAllById(List.of("20260001"))).thenReturn(List.of());
        when(posterCacheRepository.findAllByMovieCodeIn(List.of("20260001")))
                .thenReturn(List.of());

        MovieService service = createService(
                kobis,
                cacheRepository,
                movieRepository,
                posterCacheRepository,
                mock(MoviePersistenceService.class),
                mock(PosterFileStorageService.class)
        );

        var response = service.searchMovies("테스트 영화", 1, 20);

        assertEquals(1, response.content().size());
        assertEquals("저장된 검색 결과", response.content().get(0).title());
        verify(kobis).searchMovies("테스트 영화", 1, 20);
    }

    @Test
    void omitsMissingLocalPosterFromRetainedSearchResults() {
        KobisApiClient kobis = mock(KobisApiClient.class);
        MovieCacheRepository cacheRepository = mock(MovieCacheRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MoviePosterCacheRepository posterCacheRepository =
                mock(MoviePosterCacheRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);

        Movie savedMovie = new Movie(
                "20260003", "포스터 파일이 유실된 영화", "", "20260101",
                "", "드라마", "테스트 감독", "", ""
        );
        savedMovie.updatePosterUrl("/posters/missing-poster.jpg");

        MovieCache stale = expiredCache(
                "SEARCH",
                "포스터 유실|1|20",
                """
                {
                  "content": [{
                    "movieCode": "20260003",
                    "title": "포스터 파일이 유실된 영화",
                    "releaseDate": "20260101",
                    "genre": "드라마",
                    "director": "테스트 감독",
                    "posterUrl": "/posters/missing-poster.jpg"
                  }],
                  "page": 1,
                  "size": 20,
                  "totalCount": 1,
                  "hasNext": false
                }
                """
        );

        when(cacheRepository.findByCacheTypeAndCacheKey("SEARCH", "포스터 유실|1|20"))
                .thenReturn(Optional.of(stale));
        when(kobis.searchMovies("포스터 유실", 1, 20))
                .thenThrow(new ResourceAccessException("KOBIS unavailable"));
        when(movieRepository.findAllById(List.of("20260003")))
                .thenReturn(List.of(savedMovie));
        when(posterCacheRepository.findAllByMovieCodeIn(List.of("20260003")))
                .thenReturn(List.of());
        when(storageService.exists("/posters/missing-poster.jpg")).thenReturn(false);

        MovieService service = createService(
                kobis,
                cacheRepository,
                movieRepository,
                posterCacheRepository,
                mock(MoviePersistenceService.class),
                storageService
        );

        var response = service.searchMovies("포스터 유실", 1, 20);

        assertEquals(1, response.content().size());
        assertEquals("", response.content().get(0).posterUrl());
        verify(storageService).exists("/posters/missing-poster.jpg");
    }

    @Test
    void reusesValidPosterStoredInSearchCacheWhenDatabaseRowsAreMissing() {
        KobisApiClient kobis = mock(KobisApiClient.class);
        MovieCacheRepository cacheRepository = mock(MovieCacheRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MoviePosterCacheRepository posterCacheRepository =
                mock(MoviePosterCacheRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);

        MovieCache stale = expiredCache(
                "SEARCH",
                "캐시 포스터|1|20",
                """
                {
                  "content": [{
                    "movieCode": "20260004",
                    "title": "검색 캐시 포스터 영화",
                    "releaseDate": "20260104",
                    "genre": "드라마",
                    "director": "테스트 감독",
                    "posterUrl": "/posters/cached-search-poster.jpg"
                  }],
                  "page": 1,
                  "size": 20,
                  "totalCount": 1,
                  "hasNext": false
                }
                """
        );

        when(cacheRepository.findByCacheTypeAndCacheKey("SEARCH", "캐시 포스터|1|20"))
                .thenReturn(Optional.of(stale));
        when(kobis.searchMovies("캐시 포스터", 1, 20))
                .thenThrow(new ResourceAccessException("KOBIS unavailable"));
        when(movieRepository.findAllById(List.of("20260004"))).thenReturn(List.of());
        when(posterCacheRepository.findAllByMovieCodeIn(List.of("20260004")))
                .thenReturn(List.of());
        when(storageService.exists("/posters/cached-search-poster.jpg")).thenReturn(true);

        MovieService service = createService(
                kobis,
                cacheRepository,
                movieRepository,
                posterCacheRepository,
                mock(MoviePersistenceService.class),
                storageService
        );

        var response = service.searchMovies("캐시 포스터", 1, 20);

        assertEquals("/posters/cached-search-poster.jpg",
                response.content().get(0).posterUrl());
        verify(storageService).exists("/posters/cached-search-poster.jpg");
    }

    @Test
    void servesRetainedDailyBoxOfficeWhenKobisIsUnavailable() {
        KobisApiClient kobis = mock(KobisApiClient.class);
        MovieCacheRepository cacheRepository = mock(MovieCacheRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MoviePosterCacheRepository posterCacheRepository =
                mock(MoviePosterCacheRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);
        String date = "20261112";
        MovieCache stale = expiredCache(
                "DAILY_BOX_OFFICE",
                date,
                """
                [{
                  "rank": 1,
                  "movieCode": "20260002",
                  "title": "저장된 박스오피스",
                  "releaseDate": "20261101",
                  "audienceCount": "100",
                  "totalAudience": "1000",
                  "posterUrl": "/posters/box-office.jpg"
                }]
                """
        );

        when(cacheRepository.findByCacheTypeAndCacheKey("DAILY_BOX_OFFICE", date))
                .thenReturn(Optional.of(stale));
        when(kobis.getDailyBoxOffice(date))
                .thenThrow(new ResourceAccessException("KOBIS unavailable"));
        when(storageService.exists("/posters/box-office.jpg")).thenReturn(true);
        when(movieRepository.findAllById(List.of("20260002"))).thenReturn(List.of());
        when(posterCacheRepository.findAllByMovieCodeIn(List.of("20260002")))
                .thenReturn(List.of());

        MovieService service = createService(
                kobis,
                cacheRepository,
                movieRepository,
                posterCacheRepository,
                mock(MoviePersistenceService.class),
                storageService
        );

        var response = service.getDailyBoxOffice(date);

        assertEquals(1, response.size());
        assertEquals("저장된 박스오피스", response.get(0).title());
        assertEquals("/posters/box-office.jpg", response.get(0).posterUrl());
        verify(kobis).getDailyBoxOffice(date);
    }
}
