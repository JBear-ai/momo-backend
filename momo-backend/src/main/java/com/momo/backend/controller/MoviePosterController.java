
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

    private final MovieService movieService;
    private final MoviePosterCacheService posterCacheService;

    // =========================================================
    // 1. 영화 코드로 포스터 조회
    // =========================================================

    @GetMapping("/{movieCode}/poster")
    public ResponseEntity<MoviePosterResponse> getMoviePoster(
            @PathVariable String movieCode
    ) {

        return ResponseEntity.ok(
                movieService.getMoviePoster(movieCode)
        );
    }

    // =========================================================
    // 2. 여러 영화의 포스터 상태 일괄 조회
    // =========================================================

    @PostMapping("/posters/status")
    public ResponseEntity<List<MoviePosterStatusResponse>> getPosterStatuses(
            @RequestBody List<String> movieCodes
    ) {

        if (movieCodes == null || movieCodes.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<MoviePosterStatusResponse> responses =
                posterCacheService.findPosterStatuses(movieCodes);

        return ResponseEntity.ok(responses);
    }
}
