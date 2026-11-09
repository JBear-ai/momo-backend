package com.momo.backend.service;

import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MoviePosterPersistenceServiceTest {

    @Mock
    private MoviePosterCacheRepository posterCacheRepository;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MoviePosterPersistenceService persistenceService;

    @Test
    void rejectsNonHttpPosterAddressBeforeDatabaseWrite() {
        assertThrows(
                IllegalArgumentException.class,
                () -> persistenceService.saveSuccess(
                        "12345678", "claim-1",
                        "javascript:alert(1)"
                )
        );

        verifyNoInteractions(posterCacheRepository, movieRepository);
    }

    @Test
    void rejectsPosterAddressContainingCredentials() {
        assertThrows(
                IllegalArgumentException.class,
                () -> persistenceService.saveSuccess(
                        "12345678", "claim-1",
                        "https://user:password@example.com/poster.jpg"
                )
        );

        verifyNoInteractions(posterCacheRepository, movieRepository);
    }

    @Test
    void acceptsHttpAndHttpsPosterAddresses() {
        when(posterCacheRepository.completeClaim(
                org.mockito.ArgumentMatchers.eq("12345678"),
                org.mockito.ArgumentMatchers.eq("claim-1"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(1);

        assertDoesNotThrow(() -> persistenceService.saveSuccess(
                "12345678", "claim-1", "https://image.tmdb.org/t/p/w500/poster.jpg"));

        verify(posterCacheRepository).completeClaim(
                org.mockito.ArgumentMatchers.eq("12345678"),
                org.mockito.ArgumentMatchers.eq("claim-1"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
        verify(movieRepository).findById("12345678");
    }
    @Test
    void doesNotUpdateMovieWhenClaimIsNoLongerOwned() {
        when(posterCacheRepository.completeClaim(
                org.mockito.ArgumentMatchers.eq("12345678"),
                org.mockito.ArgumentMatchers.eq("old-claim"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(0);

        org.junit.jupiter.api.Assertions.assertFalse(persistenceService.saveSuccess(
                "12345678", "old-claim", "https://image.tmdb.org/t/p/w500/poster.jpg"));

        verifyNoInteractions(movieRepository);
    }

}
