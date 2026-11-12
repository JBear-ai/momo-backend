package com.momo.backend.controller;

import com.momo.backend.dto.movie.MoviePosterStatusResponse;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.service.MoviePosterCacheService;
import com.momo.backend.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class MoviePosterControllerTest {

    @Test
    void ensuresPostersForAllValidMovieCodesAndReturnsTheirStatuses() {
        MovieService movieService = mock(MovieService.class);
        MoviePosterCacheService cacheService = mock(MoviePosterCacheService.class);
        MoviePosterController controller = new MoviePosterController(movieService, cacheService);

        List<String> codes = List.of("20261101", "2026A101");
        List<MoviePosterStatusResponse> statuses = List.of(
                new MoviePosterStatusResponse("20261101", PosterStatus.SUCCESS, "/posters/one.jpg"),
                new MoviePosterStatusResponse("2026A101", PosterStatus.PROCESSING, "")
        );
        when(cacheService.findPosterStatuses(codes)).thenReturn(statuses);

        ResponseEntity<List<MoviePosterStatusResponse>> response =
                controller.ensurePosterCollection(codes);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(statuses, response.getBody());
        verify(movieService).getMoviePoster("20261101");
        verify(movieService).getMoviePoster("2026A101");
        verify(cacheService).findPosterStatuses(codes);
    }

    @Test
    void rejectsInvalidMovieCodesBeforeStartingPosterCollection() {
        MovieService movieService = mock(MovieService.class);
        MoviePosterCacheService cacheService = mock(MoviePosterCacheService.class);
        MoviePosterController controller = new MoviePosterController(movieService, cacheService);

        ResponseEntity<List<MoviePosterStatusResponse>> response =
                controller.ensurePosterCollection(List.of("20261101", "../path"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(movieService, cacheService);
    }
}
