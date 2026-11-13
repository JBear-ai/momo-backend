package com.momo.backend.scheduler;

import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.service.MoviePosterCacheService;
import com.momo.backend.service.PosterFileStorageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class MoviePosterIntegritySchedulerTest {

    @Test
    void clearsMissingPosterPathAndQueuesAutomaticRecovery() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);
        MoviePosterCacheService cacheService = mock(MoviePosterCacheService.class);

        Movie movie = new Movie(
                "20260001", "테스트 영화", "", "20260101",
                "", "드라마", "", "", ""
        );
        movie.updatePosterUrl("/posters/missing.jpg");

        when(movieRepository.findTop100ByPosterUrlStartingWithOrderByMovieCodeAsc("/posters/"))
                .thenReturn(List.of(movie));
        when(movieRepository.findTop100ByPosterUrlStartingWithAndMovieCodeGreaterThanOrderByMovieCodeAsc(
                "/posters/", "20260001"))
                .thenReturn(List.of());
        when(storageService.exists("/posters/missing.jpg")).thenReturn(false);

        MoviePosterIntegrityScheduler scheduler = new MoviePosterIntegrityScheduler(
                movieRepository, storageService, cacheService
        );

        scheduler.repairMissingPosterFiles();

        // 실제 DB 경로 변경과 재수집 큐 등록은 캐시 서비스의 단일 트랜잭션에서 처리한다.
        verify(movieRepository, never()).save(any(Movie.class));
        verify(cacheService).repairMissingPosterFile(
                "20260001", "테스트 영화", "20260101");
        verify(cacheService, never()).resetMissingPoster(
                anyString(), anyString(), anyString());
    }

    @Test
    void keepsPosterWhenTheLocalFileExists() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        PosterFileStorageService storageService = mock(PosterFileStorageService.class);
        MoviePosterCacheService cacheService = mock(MoviePosterCacheService.class);

        Movie movie = new Movie(
                "20260002", "정상 영화", "", "20260102",
                "", "드라마", "", "", ""
        );
        movie.updatePosterUrl("/posters/exists.jpg");

        when(movieRepository.findTop100ByPosterUrlStartingWithOrderByMovieCodeAsc("/posters/"))
                .thenReturn(List.of(movie));
        when(movieRepository.findTop100ByPosterUrlStartingWithAndMovieCodeGreaterThanOrderByMovieCodeAsc(
                "/posters/", "20260002"))
                .thenReturn(List.of());
        when(storageService.exists("/posters/exists.jpg")).thenReturn(true);

        MoviePosterIntegrityScheduler scheduler = new MoviePosterIntegrityScheduler(
                movieRepository, storageService, cacheService
        );

        scheduler.repairMissingPosterFiles();

        verify(movieRepository, never()).save(any(Movie.class));
        verifyNoInteractions(cacheService);
        org.junit.jupiter.api.Assertions.assertEquals("/posters/exists.jpg", movie.getPosterUrl());
    }
}
