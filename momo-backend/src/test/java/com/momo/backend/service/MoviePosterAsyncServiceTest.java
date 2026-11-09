package com.momo.backend.service;

import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class MoviePosterAsyncServiceTest {

    @Test
    void usesStoredEnglishTitleAndDirectorWhenSearchingPoster() {
        KmdbService kmdbService = mock(KmdbService.class);
        MoviePosterCacheService cacheService = mock(MoviePosterCacheService.class);
        MoviePosterPersistenceService persistenceService =
                mock(MoviePosterPersistenceService.class);
        MovieRepository movieRepository = mock(MovieRepository.class);

        Movie movie = new Movie(
                "20260001", "테스트 영화", "Test Movie", "20260101",
                "120", "드라마", "홍길동", "배우1,배우2", "12세이상관람가"
        );

        when(movieRepository.findById("20260001")).thenReturn(Optional.of(movie));
        when(kmdbService.findPosterUrl(
                "20260001", "테스트 영화", "Test Movie", "20260101", "홍길동"
        )).thenReturn("https://example.com/poster.jpg");

        MoviePosterAsyncService service = new MoviePosterAsyncService(
                kmdbService, cacheService, persistenceService, movieRepository
        );

        when(persistenceService.saveSuccess(
                "20260001", "claim-1", "https://example.com/poster.jpg"
        )).thenReturn(true);

        service.searchPosterAsync("20260001", "테스트 영화", "20260101", "claim-1");

        verify(kmdbService).findPosterUrl(
                "20260001", "테스트 영화", "Test Movie", "20260101", "홍길동"
        );
        verify(persistenceService).saveSuccess(
                "20260001", "claim-1", "https://example.com/poster.jpg"
        );
        verify(cacheService, never()).markAsFailed("20260001", "claim-1");
    }

    @Test
    void marksCacheFailedOnlyWhenBothProvidersReturnNoPoster() {
        KmdbService kmdbService = mock(KmdbService.class);
        MoviePosterCacheService cacheService = mock(MoviePosterCacheService.class);
        MoviePosterPersistenceService persistenceService =
                mock(MoviePosterPersistenceService.class);
        MovieRepository movieRepository = mock(MovieRepository.class);

        when(movieRepository.findById("20260002")).thenReturn(Optional.empty());
        when(kmdbService.findPosterUrl(
                "20260002", "포스터 없는 영화", "", "20260101", ""
        )).thenReturn(null);

        MoviePosterAsyncService service = new MoviePosterAsyncService(
                kmdbService, cacheService, persistenceService, movieRepository
        );

        service.searchPosterAsync("20260002", "포스터 없는 영화", "20260101", "claim-2");

        verify(cacheService).markAsFailed("20260002", "claim-2");
        verifyNoInteractions(persistenceService);
    }

    @Test
    void marksCacheFailedWhenAnUnexpectedErrorOccurs() {
        KmdbService kmdbService = mock(KmdbService.class);
        MoviePosterCacheService cacheService = mock(MoviePosterCacheService.class);
        MoviePosterPersistenceService persistenceService =
                mock(MoviePosterPersistenceService.class);
        MovieRepository movieRepository = mock(MovieRepository.class);

        when(movieRepository.findById("20260003"))
                .thenThrow(new IllegalStateException("temporary database error"));

        MoviePosterAsyncService service = new MoviePosterAsyncService(
                kmdbService, cacheService, persistenceService, movieRepository
        );

        service.searchPosterAsync("20260003", "오류 테스트", "20260101", "claim-3");

        verify(cacheService).markAsFailed("20260003", "claim-3");
        verifyNoInteractions(persistenceService);
    }
    @Test
    void ignoresPosterResultWhenClaimWasTakenOverByAnotherWorker() {
        KmdbService kmdbService = mock(KmdbService.class);
        MoviePosterCacheService cacheService = mock(MoviePosterCacheService.class);
        MoviePosterPersistenceService persistenceService = mock(MoviePosterPersistenceService.class);
        MovieRepository movieRepository = mock(MovieRepository.class);

        when(movieRepository.findById("20260004")).thenReturn(Optional.empty());
        when(kmdbService.findPosterUrl("20260004", "오래된 작업", "", "20260101", ""))
                .thenReturn("https://example.com/old-poster.jpg");
        when(persistenceService.saveSuccess(
                "20260004", "old-claim", "https://example.com/old-poster.jpg"
        )).thenReturn(false);

        MoviePosterAsyncService service = new MoviePosterAsyncService(
                kmdbService, cacheService, persistenceService, movieRepository
        );

        service.searchPosterAsync("20260004", "오래된 작업", "20260101", "old-claim");

        verify(persistenceService).saveSuccess(
                "20260004", "old-claim", "https://example.com/old-poster.jpg"
        );
        verify(cacheService, never()).markAsFailed(anyString(), anyString());
    }

}
