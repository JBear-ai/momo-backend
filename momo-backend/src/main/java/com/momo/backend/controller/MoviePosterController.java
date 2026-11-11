package com.momo.backend.controller;

import com.momo.backend.dto.movie.MoviePosterResponse;
import com.momo.backend.dto.movie.MoviePosterStatusResponse;
import com.momo.backend.service.MoviePosterCacheService;
import com.momo.backend.service.MovieService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MoviePosterController {

    private static final int MAX_STATUS_CODES = 20;

    private final MovieService movieService;
    private final MoviePosterCacheService posterCacheService;

    @GetMapping("/{movieCode:[A-Za-z0-9]{8}}/poster")
    public ResponseEntity<MoviePosterResponse> getMoviePoster(
            @PathVariable String movieCode) {

        return ResponseEntity.ok(movieService.getMoviePoster(movieCode));
    }

    @PostMapping("/posters/status")
    public ResponseEntity<List<MoviePosterStatusResponse>> getPosterStatuses(
            @RequestBody(required = false) List<String> movieCodes) {

        if (movieCodes == null
                || movieCodes.isEmpty()
                || movieCodes.size() > MAX_STATUS_CODES
                || movieCodes.stream().anyMatch(code ->
                        code == null || !code.matches("[A-Za-z0-9]{8}"))) {
            return ResponseEntity.badRequest().build();
        }

        List<String> distinctMovieCodes = movieCodes.stream()
                .distinct()
                .toList();

        return ResponseEntity.ok(
                posterCacheService.findPosterStatuses(distinctMovieCodes)
        );
    }
}
