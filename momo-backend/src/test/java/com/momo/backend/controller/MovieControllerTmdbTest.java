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
import java.util.Map;

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

        TmdbMovieDetailResponse expected = tmdbDetail(null);
        when(tmdbService.getMovieDetailById(555094L)).thenReturn(expected);
        when(personService.resolveFilmographyMovie(555094L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "KOBIS not found"));

        ResponseEntity<?> response = controller.getMovieDetailByTmdbId(555094L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(movieService, never()).getMovieDetail(anyString());
    }

    @Test
    void mergesKobisMovieCodeWithoutLosingTmdbPersonIds() {
        ReviewService reviewService = mock(ReviewService.class);
        MovieService movieService = mock(MovieService.class);
        PersonService personService = mock(PersonService.class);
        TmdbService tmdbService = mock(TmdbService.class);
        MovieController controller = new MovieController(
                reviewService, movieService, personService, tmdbService);

        TmdbMovieDetailResponse tmdb = tmdbDetail(null);
        MovieDetailResponse kobis = new MovieDetailResponse(
                "20261234", "KOBIS title", "KOBIS original", "20261212",
                "110", "Drama", "KOBIS director", List.of("KOBIS actor"), "15", "KOBIS overview");

        when(tmdbService.getMovieDetailById(555094L)).thenReturn(tmdb);
        when(personService.resolveFilmographyMovie(555094L)).thenReturn("20261234");
        when(movieService.getMovieDetail("20261234")).thenReturn(kobis);

        ResponseEntity<?> response = controller.getMovieDetailByTmdbId(555094L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(TmdbMovieDetailResponse.class, response.getBody());

        TmdbMovieDetailResponse merged = (TmdbMovieDetailResponse) response.getBody();
        assertEquals("20261234", merged.movieCode());
        assertEquals("KOBIS title", merged.title());
        assertEquals("KOBIS overview", merged.overview());
        assertEquals(1, merged.people().size());
        assertEquals(20L, ((Number) merged.people().get(0).get("personId")).longValue());
    }

    @Test
    void keepsTmdbDetailsAndMovieCodeWhenKobisDetailLookupFails() {
        ReviewService reviewService = mock(ReviewService.class);
        MovieService movieService = mock(MovieService.class);
        PersonService personService = mock(PersonService.class);
        TmdbService tmdbService = mock(TmdbService.class);
        MovieController controller = new MovieController(
                reviewService, movieService, personService, tmdbService);

        TmdbMovieDetailResponse expected = tmdbDetail(null);
        when(tmdbService.getMovieDetailById(555094L)).thenReturn(expected);
        when(personService.resolveFilmographyMovie(555094L)).thenReturn("20261234");
        when(movieService.getMovieDetail("20261234"))
                .thenThrow(new IllegalStateException("KOBIS detail is temporarily unavailable"));

        ResponseEntity<?> response = controller.getMovieDetailByTmdbId(555094L);

        TmdbMovieDetailResponse result = (TmdbMovieDetailResponse) response.getBody();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("20261234", result.movieCode());
        assertEquals(expected.title(), result.title());
        assertEquals(1, result.people().size());
    }

    @Test
    void rejectsInvalidTmdbIdBeforeCallingExternalApis() {
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

    private TmdbMovieDetailResponse tmdbDetail(String movieCode) {
        Map<String, Object> person = Map.of(
                "personId", 20L,
                "name", "TMDb actor",
                "department", "actor",
                "profileUrl", "https://image.tmdb.org/t/p/w500/actor.jpg");

        return new TmdbMovieDetailResponse(
                555094L, movieCode, "TMDb title", "Original title", "20261211",
                "112", "Animation", "TMDb director", List.of("TMDb actor"), "12",
                "TMDb overview", "https://image.tmdb.org/t/p/w500/poster.jpg", List.of(person));
    }
}
