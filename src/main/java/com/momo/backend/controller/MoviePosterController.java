
package com.momo.backend.controller;

import com.momo.backend.dto.movie.MoviePosterResponse;
import com.momo.backend.service.KmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MoviePosterController {

    private final KmdbService kmdbService;

    // 영화 코드로 포스터 조회
    @GetMapping("/{movieCode}/poster")
    public ResponseEntity<MoviePosterResponse> getMoviePoster(
            @PathVariable String movieCode) {

        return ResponseEntity.ok(
                kmdbService.getMoviePoster(movieCode)
        );
    }
}
