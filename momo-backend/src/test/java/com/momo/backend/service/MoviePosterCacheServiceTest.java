package com.momo.backend.service;

import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MoviePosterCacheServiceTest {

    @Test
    void returnsUniqueClaimTokenWhenJobIsClaimed() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        when(repository.tryClaim(
                eq("20260001"), anyString(), any(LocalDateTime.class),
                any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(1);

        MoviePosterCacheService service = new MoviePosterCacheService(
                repository, mock(MovieRepository.class), mock(PosterFileStorageService.class));

        String token = service.tryMarkAsProcessing("20260001");

        assertNotNull(token);
        assertEquals(36, token.length());
        verify(repository).tryClaim(
                eq("20260001"), eq(token), any(LocalDateTime.class),
                any(LocalDateTime.class), any(LocalDateTime.class)
        );
    }

    @Test
    void resetsMissingPosterCacheWithoutCreatingDuplicateRows() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        when(repository.existsById("20260005")).thenReturn(true);
        MoviePosterCacheService service =
                new MoviePosterCacheService(
                repository, mock(MovieRepository.class), mock(PosterFileStorageService.class));

        service.resetMissingPoster("20260005", "테스트 영화", "20260101");

        verify(repository).resetMissingPoster(
                eq("20260005"),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(repository, never()).insertIfAbsent(
                anyString(), anyString(), anyString(), anyInt(), any(LocalDateTime.class));
    }

    @Test
    void reportsFailedWhenSuccessfulCacheContainsOnlyAnExternalUrl() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);
        MoviePosterCache cache = new MoviePosterCache(
                "20260009", "테스트 영화", "20260101");
        ReflectionTestUtils.setField(cache, "status", PosterStatus.SUCCESS);
        ReflectionTestUtils.setField(
                cache, "posterUrl", "https://image.tmdb.org/t/p/w500/old-poster.jpg");
        when(repository.findAllByMovieCodeIn(anyCollection()))
                .thenReturn(List.of(cache));

        MoviePosterCacheService service =
                new MoviePosterCacheService(repository, mock(MovieRepository.class), storageService);

        var statuses = service.findPosterStatuses(List.of("20260009"));

        assertEquals(1, statuses.size());
        assertEquals(PosterStatus.FAILED, statuses.get(0).status());
        assertEquals("", statuses.get(0).posterUrl());
        verifyNoInteractions(storageService);
    }

    @Test
    void reportsFailedWhenSuccessfulCachePointsToAMissingLocalFile() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);
        MoviePosterCache cache = new MoviePosterCache(
                "20260013", "파일이 유실된 영화", "20260101");
        ReflectionTestUtils.setField(cache, "status", PosterStatus.SUCCESS);
        ReflectionTestUtils.setField(cache, "posterUrl", "/posters/20260013-missing.jpg");

        when(repository.findAllByMovieCodeIn(List.of("20260013")))
                .thenReturn(List.of(cache));
        when(movieRepository.findAllById(List.of("20260013"))).thenReturn(List.of());
        when(storageService.exists("/posters/20260013-missing.jpg")).thenReturn(false);

        MoviePosterCacheService service = new MoviePosterCacheService(
                repository, movieRepository, storageService);

        var statuses = service.findPosterStatuses(List.of("20260013"));

        assertEquals(PosterStatus.FAILED, statuses.get(0).status());
        assertEquals("", statuses.get(0).posterUrl());
        verify(storageService).exists("/posters/20260013-missing.jpg");
    }

    @Test
    void singlePosterStatusDoesNotReportMissingFileAsSuccess() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);
        MoviePosterCache cache = new MoviePosterCache(
                "20260014", "파일이 유실된 영화", "20260101");
        ReflectionTestUtils.setField(cache, "status", PosterStatus.SUCCESS);
        ReflectionTestUtils.setField(cache, "posterUrl", "/posters/20260014-missing.jpg");

        when(repository.findById("20260014")).thenReturn(java.util.Optional.of(cache));
        when(storageService.exists("/posters/20260014-missing.jpg")).thenReturn(false);

        MoviePosterCacheService service = new MoviePosterCacheService(
                repository, mock(MovieRepository.class), storageService);

        var status = service.findPosterStatus("20260014").orElseThrow();

        assertEquals(PosterStatus.FAILED, status.status());
        assertEquals("", status.posterUrl());
    }

    @Test
    void exposesSavedMoviePosterEvenWhenPosterCacheRowDoesNotExist() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);
        Movie movie = new Movie(
                "20260010", "테스트 영화", "", "20260101", "", "", "", "", "");
        movie.updatePosterUrl("/posters/20260010-existing.jpg");

        when(movieRepository.findAllById(List.of("20260010")))
                .thenReturn(List.of(movie));
        when(repository.findAllByMovieCodeIn(List.of("20260010")))
                .thenReturn(List.of());
        when(storageService.exists("/posters/20260010-existing.jpg"))
                .thenReturn(true);

        MoviePosterCacheService service =
                new MoviePosterCacheService(repository, movieRepository, storageService);

        var statuses = service.findPosterStatuses(List.of("20260010"));

        assertEquals(1, statuses.size());
        assertEquals(PosterStatus.SUCCESS, statuses.get(0).status());
        assertEquals("/posters/20260010-existing.jpg", statuses.get(0).posterUrl());
    }

    @Test
    void reportsPendingWhenPosterEnsureWasQueuedButCacheCreationHasNotFinished() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        Movie movie = new Movie(
                "20260012", "비동기 포스터 확인 영화", "", "20260101", "", "", "", "", "");

        when(movieRepository.findAllById(List.of("20260012")))
                .thenReturn(List.of(movie));
        when(repository.findAllByMovieCodeIn(List.of("20260012")))
                .thenReturn(List.of());

        MoviePosterCacheService service = new MoviePosterCacheService(
                repository, movieRepository, mock(PosterFileStorageService.class));

        var statuses = service.findPosterStatuses(
                List.of("20260012"), PosterStatus.PENDING);

        assertEquals(1, statuses.size());
        assertEquals(PosterStatus.PENDING, statuses.get(0).status());
        assertEquals("", statuses.get(0).posterUrl());
    }

    @Test
    void returnsFailedForMoviesWithoutPosterCacheOrSavedPoster() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        Movie movie = new Movie(
                "20260011", "포스터 없는 영화", "", "20260101", "", "", "", "", "");

        when(movieRepository.findAllById(List.of("20260011")))
                .thenReturn(List.of(movie));
        when(repository.findAllByMovieCodeIn(List.of("20260011")))
                .thenReturn(List.of());

        MoviePosterCacheService service = new MoviePosterCacheService(
                repository, movieRepository, mock(PosterFileStorageService.class));

        var statuses = service.findPosterStatuses(List.of("20260011"));

        assertEquals(1, statuses.size());
        assertEquals(PosterStatus.FAILED, statuses.get(0).status());
        assertEquals("", statuses.get(0).posterUrl());
    }

    @Test
    void returnsNullWhenAnotherWorkerOwnsTheClaim() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        when(repository.tryClaim(
                anyString(), anyString(), any(LocalDateTime.class),
                any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(0);

        MoviePosterCacheService service = new MoviePosterCacheService(
                repository, mock(MovieRepository.class), mock(PosterFileStorageService.class));

        assertNull(service.tryMarkAsProcessing("20260001"));
    }

    @Test
    void releasesOnlyTheClaimOwnedByTheGivenToken() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MoviePosterCacheService service = new MoviePosterCacheService(
                repository, mock(MovieRepository.class), mock(PosterFileStorageService.class));

        service.releaseProcessingClaim("20260001", "claim-token");

        verify(repository).releaseClaim(
                eq("20260001"), eq("claim-token"), any(LocalDateTime.class));
    }

    @Test
    void doesNotReleaseWhenClaimTokenIsMissing() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MoviePosterCacheService service = new MoviePosterCacheService(
                repository, mock(MovieRepository.class), mock(PosterFileStorageService.class));

        service.releaseProcessingClaim("20260001", null);

        verifyNoInteractions(repository);
    }
}
