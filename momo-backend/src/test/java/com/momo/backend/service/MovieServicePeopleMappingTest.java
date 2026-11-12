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
}
