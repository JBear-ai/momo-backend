package com.momo.backend.service;

import com.momo.backend.dto.movie.MovieSearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

class MoviePersistenceServiceTest {

    @Test
    void removesDuplicateMovieCodesFromTheSameKobisResponse() {
        MoviePersistenceWriter writer = mock(MoviePersistenceWriter.class);
        MoviePersistenceService service = new MoviePersistenceService(writer);

        service.saveSearchMovies(List.of(
                new MovieSearchResponse("20260001", "첫 번째 제목", "20260101", "드라마", "감독 A"),
                new MovieSearchResponse("20260001", "중복 제목", "20260101", "드라마", "감독 A")
        ));

        verify(writer).saveOrSupplement(argThat(movie ->
                "20260001".equals(movie.movieCode())
                        && "첫 번째 제목".equals(movie.title())
        ));
        verifyNoMoreInteractions(writer);
    }

    @Test
    void skipsInvalidMovieRowsWithoutCallingTheWriter() {
        MoviePersistenceWriter writer = mock(MoviePersistenceWriter.class);
        MoviePersistenceService service = new MoviePersistenceService(writer);

        service.saveSearchMovies(Arrays.asList(
                null,
                new MovieSearchResponse("", "제목", "", "", ""),
                new MovieSearchResponse("20260002", " ", "", "", "")
        ));

        verifyNoInteractions(writer);
    }

    @Test
    void recoversConcurrentPrimaryKeyConflictInANewTransaction() {
        MoviePersistenceWriter writer = mock(MoviePersistenceWriter.class);
        MovieSearchResponse movie = new MovieSearchResponse(
                "20260003", "동시 저장 영화", "20260103", "드라마", "감독 B"
        );
        doThrow(new DataIntegrityViolationException("duplicate movieCode"))
                .when(writer).saveOrSupplement(movie);

        MoviePersistenceService service = new MoviePersistenceService(writer);
        service.saveSearchMovies(List.of(movie));

        verify(writer).saveOrSupplement(movie);
        verify(writer).supplementAfterConcurrentInsert(movie);
        verifyNoMoreInteractions(writer);
    }

    @Test
    void continuesWithOtherMoviesAfterRecoveringOneDuplicateInsert() {
        MoviePersistenceWriter writer = mock(MoviePersistenceWriter.class);
        MovieSearchResponse first = new MovieSearchResponse(
                "20260004", "동시 저장 영화", "20260104", "드라마", "감독 C"
        );
        MovieSearchResponse second = new MovieSearchResponse(
                "20260005", "다음 영화", "20260105", "액션", "감독 D"
        );
        doThrow(new DataIntegrityViolationException("duplicate movieCode"))
                .when(writer).saveOrSupplement(first);

        MoviePersistenceService service = new MoviePersistenceService(writer);
        service.saveSearchMovies(List.of(first, second));

        verify(writer).supplementAfterConcurrentInsert(first);
        verify(writer).saveOrSupplement(second);
    }
}
