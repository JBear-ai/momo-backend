package com.momo.backend.service;

import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MoviePosterCache;
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
import static org.mockito.Mockito.never;

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
    void savesFilmographyTmdbPosterForTheResolvedAlphanumericMovieCode() {
        String movieCode = "2026A194";
        String tmdbPosterUrl = "https://image.tmdb.org/t/p/w500/nintama.jpg";
        String localPosterPath = "/posters/2026A194-tmdb.jpg";
        Movie movie = new Movie(
                movieCode,
                "극장판 닌자보이 란타로: 도쿠타케 닌자대 최강의 군사",
                "NINTAMA RANTARO",
                "20250514",
                "89",
                "애니메이션",
                "",
                "",
                "전체관람가"
        );
        MoviePosterCache pendingCache = new MoviePosterCache(
                movieCode, movie.getTitle(), movie.getReleaseDate());

        when(movieRepository.findById(movieCode)).thenReturn(
                java.util.Optional.of(movie));
        when(posterCacheRepository.findById(movieCode))
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.of(pendingCache));
        when(posterCacheRepository.tryClaim(
                eq(movieCode), anyString(), any(), any(), any()))
                .thenReturn(1);
        when(posterFileStorageService.downloadAndSave(movieCode, tmdbPosterUrl))
                .thenReturn(localPosterPath);
        when(posterCacheRepository.completeClaim(
                eq(movieCode), anyString(), eq(localPosterPath), any()))
                .thenReturn(1);

        boolean saved = persistenceService.saveFilmographyPosterIfMissing(
                movieCode, tmdbPosterUrl);

        org.junit.jupiter.api.Assertions.assertTrue(saved);
        verify(posterCacheRepository).insertIfAbsent(
                eq(movieCode), eq(movie.getTitle()), eq(movie.getReleaseDate()),
                eq(50), any());
        verify(posterFileStorageService).downloadAndSave(movieCode, tmdbPosterUrl);
        verify(posterCacheRepository).completeClaim(
                eq(movieCode), anyString(), eq(localPosterPath), any());
        verify(movieRepository).save(movie);
    }

    @Test
    void keepsAnExistingLocalPosterInsteadOfOverwritingItWithFilmographyPoster() {
        Movie movie = new Movie(
                "2026A194", "테스트 영화", "", "20250514", "", "", "", "", "");
        movie.updatePosterUrl("/posters/existing.jpg");
        when(movieRepository.findById("2026A194"))
                .thenReturn(java.util.Optional.of(movie));
        when(posterFileStorageService.exists("/posters/existing.jpg"))
                .thenReturn(true);

        boolean saved = persistenceService.saveFilmographyPosterIfMissing(
                "2026A194", "https://image.tmdb.org/t/p/w500/new.jpg");

        org.junit.jupiter.api.Assertions.assertTrue(saved);
        verify(posterCacheRepository, never()).insertIfAbsent(
                anyString(), anyString(), any(), any(Integer.class), any());
        verify(posterFileStorageService, never()).downloadAndSave(
                anyString(), anyString());
    }

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
