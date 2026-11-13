package com.momo.backend.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.momo.backend.client.KobisApiClient;
import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieCacheRepository;
import com.momo.backend.repository.MovieExternalIdMappingRepository;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MovieServiceConcurrentDetailTest {

    @Test
    void concurrentRequestsForAnUnregisteredMovieCallKobisAndInsertOnlyOnce() throws Exception {
        String movieCode = "20261234";
        String kobisResponse = """
                {
                  "movieInfoResult": {
                    "movieInfo": {
                      "movieCd": "20261234",
                      "movieNm": "동시 상세 요청 영화",
                      "movieNmEn": "Concurrent Detail Movie",
                      "openDt": "20261112",
                      "showTm": "110",
                      "genres": [{"genreNm": "드라마"}],
                      "directors": [{"peopleNm": "테스트 감독"}],
                      "actors": [{"peopleNm": "테스트 배우"}],
                      "audits": [{"watchGradeNm": "12세 관람가"}]
                    }
                  }
                }
                """;

        MovieRepository movieRepository = mock(MovieRepository.class);
        KobisApiClient kobisApiClient = mock(KobisApiClient.class);
        TmdbService tmdbService = mock(TmdbService.class);
        AtomicReference<Movie> persistedMovie = new AtomicReference<>();
        CountDownLatch firstKobisRequestStarted = new CountDownLatch(1);

        when(movieRepository.findById(movieCode))
                .thenAnswer(invocation -> Optional.ofNullable(persistedMovie.get()));
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
            Movie movie = invocation.getArgument(0);
            persistedMovie.set(movie);
            return movie;
        });
        when(kobisApiClient.getMovieDetail(movieCode)).thenAnswer(invocation -> {
            firstKobisRequestStarted.countDown();
            Thread.sleep(100);
            return kobisResponse;
        });
        when(tmdbService.findMovieOverview("동시 상세 요청 영화", "20261112"))
                .thenReturn(Optional.empty());

        MovieService service = new MovieService(
                mock(MoviePosterAsyncService.class),
                kobisApiClient,
                mock(KmdbService.class),
                tmdbService,
                new tools.jackson.databind.ObjectMapper(),
                movieRepository,
                mock(MovieCacheRepository.class),
                mock(MoviePosterCacheService.class),
                mock(MoviePersistenceService.class),
                mock(MoviePosterCacheRepository.class),
                mock(MovieExternalIdMappingRepository.class),
                mock(PosterFileStorageService.class),
                Caffeine.newBuilder().maximumSize(20).build()
        );

        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch startTogether = new CountDownLatch(1);

            var first = executor.submit(() -> {
                startTogether.await();
                return service.getMovieDetail(movieCode);
            });
            var second = executor.submit(() -> {
                startTogether.await();
                return service.getMovieDetail(movieCode);
            });

            startTogether.countDown();
            assertEquals(true, firstKobisRequestStarted.await(2, TimeUnit.SECONDS));

            var firstResponse = first.get(5, TimeUnit.SECONDS);
            var secondResponse = second.get(5, TimeUnit.SECONDS);

            assertEquals(movieCode, firstResponse.movieCode());
            assertEquals(movieCode, secondResponse.movieCode());
            assertEquals("동시 상세 요청 영화", firstResponse.title());
            assertEquals("동시 상세 요청 영화", secondResponse.title());
        }

        verify(kobisApiClient, times(1)).getMovieDetail(movieCode);
        verify(movieRepository, times(1)).save(any(Movie.class));
    }
}
