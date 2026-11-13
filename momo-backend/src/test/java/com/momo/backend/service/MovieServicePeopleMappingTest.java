package com.momo.backend.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.momo.backend.client.KobisApiClient;
import com.momo.backend.dto.movie.TmdbMovieDetailResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MovieExternalIdMapping;
import com.momo.backend.repository.MovieCacheRepository;
import com.momo.backend.repository.MovieExternalIdMappingRepository;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MovieServicePeopleMappingTest {

    @Test
    void loadsPeopleByPreviouslyVerifiedTmdbMovieId() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieExternalIdMappingRepository mappingRepository =
                mock(MovieExternalIdMappingRepository.class);
        TmdbService tmdbService = mock(TmdbService.class);

        Movie movie = new Movie(
                "20240001", "동명 영화", "Same Title", "20240101",
                "100", "드라마", "감독", "", "12세");
        when(movieRepository.findById("20240001")).thenReturn(Optional.of(movie));
        when(mappingRepository.findFirstByProviderAndMovieCodeOrderByMappingIdAsc(
                "TMDB", "20240001"))
                .thenReturn(Optional.of(
                        new MovieExternalIdMapping("TMDB", "900001", "20240001")));

        List<Map<String, Object>> expectedPeople = List.of(
                Map.of("personId", 100L, "name", "정확한 배우")
        );
        when(tmdbService.getMovieDetailById(900001L)).thenReturn(
                new TmdbMovieDetailResponse(
                        900001L, "20240001", "동명 영화", "Same Title",
                        "20240101", "100", "드라마", "감독",
                        List.of("정확한 배우"), "12세", "", "", expectedPeople)
        );

        MovieService service = new MovieService(
                mock(MoviePosterAsyncService.class),
                mock(KobisApiClient.class),
                mock(KmdbService.class),
                tmdbService,
                new tools.jackson.databind.ObjectMapper(),
                movieRepository,
                mock(MovieCacheRepository.class),
                mock(MoviePosterCacheService.class),
                mock(MoviePersistenceService.class),
                mock(MoviePosterCacheRepository.class),
                mappingRepository,
                mock(PosterFileStorageService.class),
                Caffeine.newBuilder().maximumSize(10).build()
        );

        assertEquals(expectedPeople, service.getMoviePeople("20240001"));
        verify(tmdbService).getMovieDetailById(900001L);
        verify(tmdbService, never()).findMoviePeople(
                anyString(), anyString(), anyString(), anyString());
    } 
    @Test
    void persistsTmdbIdAfterFirstVerifiedPeopleLookup() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieExternalIdMappingRepository mappingRepository =
                mock(MovieExternalIdMappingRepository.class);
        TmdbService tmdbService = mock(TmdbService.class);
        Movie movie = new Movie(
                "20240002", "확인된 영화", "Verified Movie", "20240101",
                "100", "드라마", "감독", "", "12세");
        when(movieRepository.findById("20240002")).thenReturn(Optional.of(movie));
        when(mappingRepository.findFirstByProviderAndMovieCodeOrderByMappingIdAsc(
                "TMDB", "20240002")).thenReturn(Optional.empty());
        when(mappingRepository.findByProviderAndExternalId("TMDB", "900002"))
                .thenReturn(Optional.empty());

        List<Map<String, Object>> people = List.of(
                Map.of("personId", 101L, "name", "검증된 배우"));
        doAnswer(invocation -> {
            java.util.function.LongConsumer callback = invocation.getArgument(4);
            callback.accept(900002L);
            return people;
        }).when(tmdbService).findMoviePeople(
                eq("확인된 영화"), eq("Verified Movie"), eq("20240101"),
                eq("감독"), any(java.util.function.LongConsumer.class));

        MovieService service = new MovieService(
                mock(MoviePosterAsyncService.class),
                mock(KobisApiClient.class),
                mock(KmdbService.class),
                tmdbService,
                new tools.jackson.databind.ObjectMapper(),
                movieRepository,
                mock(MovieCacheRepository.class),
                mock(MoviePosterCacheService.class),
                mock(MoviePersistenceService.class),
                mock(MoviePosterCacheRepository.class),
                mappingRepository,
                mock(PosterFileStorageService.class),
                Caffeine.newBuilder().maximumSize(10).build()
        );

        assertEquals(people, service.getMoviePeople("20240002"));
        verify(mappingRepository).saveAndFlush(
                argThat(mapping -> mapping.getProvider().equals("TMDB")
                        && mapping.getExternalId().equals("900002")
                        && mapping.getMovieCode().equals("20240002")));
    }

}
