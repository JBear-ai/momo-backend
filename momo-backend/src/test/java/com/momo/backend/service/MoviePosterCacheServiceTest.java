package com.momo.backend.service;

import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MoviePosterCacheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import java.time.LocalDateTime;

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

        MoviePosterCacheService service = new MoviePosterCacheService(repository, mock(PosterFileStorageService.class));

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
                new MoviePosterCacheService(repository, mock(PosterFileStorageService.class));

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
    void doesNotExposeAnExternalUrlAsASuccessfulLocallyStoredPoster() {
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
                new MoviePosterCacheService(repository, storageService);

        var statuses = service.findPosterStatuses(List.of("20260009"));

        assertEquals(1, statuses.size());
        assertEquals(PosterStatus.SUCCESS, statuses.get(0).status());
        assertEquals("", statuses.get(0).posterUrl());
        verifyNoInteractions(storageService);
    }

    @Test
    void returnsNullWhenAnotherWorkerOwnsTheClaim() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        when(repository.tryClaim(
                anyString(), anyString(), any(LocalDateTime.class),
                any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(0);

        MoviePosterCacheService service = new MoviePosterCacheService(repository, mock(PosterFileStorageService.class));

        assertNull(service.tryMarkAsProcessing("20260001"));
    }

    @Test
    void releasesOnlyTheClaimOwnedByTheGivenToken() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MoviePosterCacheService service = new MoviePosterCacheService(repository, mock(PosterFileStorageService.class));

        service.releaseProcessingClaim("20260001", "claim-token");

        verify(repository).releaseClaim(
                eq("20260001"), eq("claim-token"), any(LocalDateTime.class));
    }

    @Test
    void doesNotReleaseWhenClaimTokenIsMissing() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MoviePosterCacheService service = new MoviePosterCacheService(repository, mock(PosterFileStorageService.class));

        service.releaseProcessingClaim("20260001", null);

        verifyNoInteractions(repository);
    }
}
