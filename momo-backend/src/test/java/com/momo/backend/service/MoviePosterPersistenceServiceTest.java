package com.momo.backend.service;

import com.momo.backend.repository.MoviePosterCacheRepository;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoviePosterPersistenceServiceTest {

    @Mock
    private MoviePosterCacheRepository posterCacheRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private PosterFileStorageService posterFileStorageService;

    @InjectMocks
    private MoviePosterPersistenceService persistenceService;

    @Test
    void rejectsNonHttpPosterAddressBeforeDatabaseWrite() {
        assertThrows(
                IllegalArgumentException.class,
                () -> persistenceService.saveSuccess(
                        "12345678", "claim-1", "javascript:alert(1)")
        );

        verifyNoInteractions(posterCacheRepository, movieRepository, posterFileStorageService);
    }

    @Test
    void rejectsPosterAddressContainingCredentials() {
        assertThrows(
                IllegalArgumentException.class,
                () -> persistenceService.saveSuccess(
                        "12345678", "claim-1",
                        "https://user:password@example.com/poster.jpg")
        );

        verifyNoInteractions(posterCacheRepository, movieRepository, posterFileStorageService);
    }

    @Test
    void downloadsPosterBeforeSavingLocalPathToDatabase() {
        when(posterFileStorageService.downloadAndSave(
                "12345678", "https://image.tmdb.org/t/p/w500/poster.jpg"))
                .thenReturn("/posters/12345678-image.jpg");
        when(posterCacheRepository.completeClaim(
                eq("12345678"), eq("claim-1"), eq("/posters/12345678-image.jpg"), any()))
                .thenReturn(1);

        assertDoesNotThrow(() -> persistenceService.saveSuccess(
                "12345678", "claim-1", "https://image.tmdb.org/t/p/w500/poster.jpg"));

        verify(posterFileStorageService).downloadAndSave(
                "12345678", "https://image.tmdb.org/t/p/w500/poster.jpg");
        verify(posterCacheRepository).completeClaim(
                eq("12345678"), eq("claim-1"), eq("/posters/12345678-image.jpg"), any());
        verify(movieRepository).findById("12345678");
    }

    @Test
    void deletesDownloadedFileWhenClaimIsNoLongerOwned() {
        when(posterFileStorageService.downloadAndSave(
                "12345678", "https://image.tmdb.org/t/p/w500/poster.jpg"))
                .thenReturn("/posters/12345678-old-image.jpg");
        when(posterCacheRepository.completeClaim(
                eq("12345678"), eq("old-claim"), anyString(), any()))
                .thenReturn(0);

        assertFalse(persistenceService.saveSuccess(
                "12345678", "old-claim", "https://image.tmdb.org/t/p/w500/poster.jpg"));

        verify(posterFileStorageService).delete("/posters/12345678-old-image.jpg");
        verifyNoInteractions(movieRepository);
    }
}
