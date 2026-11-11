package com.momo.backend.controller;

import com.momo.backend.dto.movie.MovieDetailResponse;
import com.momo.backend.dto.movie.TmdbMovieDetailResponse;
import com.momo.backend.service.MovieService;
import com.momo.backend.service.PersonService;
import com.momo.backend.service.ReviewService;
import com.momo.backend.service.TmdbService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovieControllerTmdbTest {

    @Test
    void fallsBackToTmdbDetailWhenKobisDoesNotContainTheMovie() {
        ReviewService reviewService = mock(ReviewService.class);
        MovieService movieService = mock(MovieService.class);
        PersonService personService = mock(PersonService.class);
        TmdbService tmdbService = mock(TmdbService.class);
        MovieController controller = new MovieController(
                reviewService, movieService, personService, tmdbService);

        TmdbMovieDetailResponse expected = new TmdbMovieDetailResponse(
                555094L, null, "TMDb title", "Original title", "20261211",
                "112", "Animation", "Director", List.of("Actor"), "12",
                "Overview", "https://image.tmdb.org/t/p/w500/poster.jpg", List.of());

        when(personService.resolveFilmographyMovie(555094L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "KOBIS not found"));
        when(tmdbService.getMovieDetailById(555094L)).thenReturn(expected);

        ResponseEntity<?> response = controller.getMovieDetailByTmdbId(555094L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(movieService, never()).getMovieDetail(anyString());
    }

    @Test
    void keepsExistingKobisMovieDetailWhenMovieCodeCanBeResolved() {
        ReviewService reviewService = mock(ReviewService.class);
        MovieService movieService = mock(MovieService.class);
        PersonService personService = mock(PersonService.class);
        TmdbService tmdbService = mock(TmdbService.class);
        MovieController controller = new MovieController(
                reviewService, movieService, personService, tmdbService);

        MovieDetailResponse expected = new MovieDetailResponse(
                "20261234", "KOBIS title", "Original title", "20261211",
                "112", "Animation", "Director", List.of("Actor"), "12", "Overview");

        when(personService.resolveFilmographyMovie(555094L)).thenReturn("20261234");
        when(movieService.getMovieDetail("20261234")).thenReturn(expected);

        ResponseEntity<?> response = controller.getMovieDetailByTmdbId(555094L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verifyNoInteractions(tmdbService);
    }

    @Test
    void rejectsInvalidTmdbIdBeforeTryingToResolveIt() {
        ReviewService reviewService = mock(ReviewService.class);
        MovieService movieService = mock(MovieService.class);
        PersonService personService = mock(PersonService.class);
        TmdbService tmdbService = mock(TmdbService.class);
        MovieController controller = new MovieController(
                reviewService, movieService, personService, tmdbService);

        assertThrows(
                ResponseStatusException.class,
                () -> controller.getMovieDetailByTmdbId(0L));
        verifyNoInteractions(personService, tmdbService);
    }
}
