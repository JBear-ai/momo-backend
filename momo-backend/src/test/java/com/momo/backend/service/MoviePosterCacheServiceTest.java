package com.momo.backend.service;

import com.momo.backend.repository.MoviePosterCacheRepository;
import org.junit.jupiter.api.Test;

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
    void returnsNullWhenAnotherWorkerOwnsTheClaim() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        when(repository.tryClaim(
                anyString(), anyString(), any(LocalDateTime.class),
                any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(0);

        MoviePosterCacheService service = new MoviePosterCacheService(repository);

        assertNull(service.tryMarkAsProcessing("20260001"));
    }

    @Test
    void releasesOnlyTheClaimOwnedByTheGivenToken() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MoviePosterCacheService service = new MoviePosterCacheService(repository);

        service.releaseProcessingClaim("20260001", "claim-token");

        verify(repository).releaseClaim(
                eq("20260001"), eq("claim-token"), any(LocalDateTime.class));
    }

    @Test
    void doesNotReleaseWhenClaimTokenIsMissing() {
        MoviePosterCacheRepository repository = mock(MoviePosterCacheRepository.class);
        MoviePosterCacheService service = new MoviePosterCacheService(repository);

        service.releaseProcessingClaim("20260001", null);

        verifyNoInteractions(repository);
    }
}
