package com.momo.backend.service;

import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MoviePersistenceServiceTest {

    @Test
    void persistsOnlyOneMovieWhenKobisResponseRepeatsTheSameMovieCode() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        when(movieRepository.findAllById(List.of("20260001"))).thenReturn(List.of());

        MoviePersistenceService service = new MoviePersistenceService(movieRepository);

        service.saveSearchMovies(List.of(
                new MovieSearchResponse(
                        "20260001", "첫 번째 제목", "20260101", "드라마", "감독 A"),
                new MovieSearchResponse(
                        "20260001", "중복 제목", "20260101", "드라마", "감독 A")
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Movie>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(movieRepository).saveAll(captor.capture());

        List<Movie> savedMovies = new java.util.ArrayList<>();
        captor.getValue().forEach(savedMovies::add);

        assertEquals(1, savedMovies.size());
        assertEquals("20260001", savedMovies.get(0).getMovieCode());
        assertEquals("첫 번째 제목", savedMovies.get(0).getTitle());
        verify(movieRepository).findAllById(List.of("20260001"));
    }

    @Test
    void skipsInvalidMovieRowsBeforeQueryingTheDatabase() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        MoviePersistenceService service = new MoviePersistenceService(movieRepository);

        service.saveSearchMovies(java.util.Arrays.asList(
                null,
                new MovieSearchResponse("", "제목", "", "", ""),
                new MovieSearchResponse("20260002", " ", "", "", "")
        ));

        verifyNoInteractions(movieRepository);
    }
}
