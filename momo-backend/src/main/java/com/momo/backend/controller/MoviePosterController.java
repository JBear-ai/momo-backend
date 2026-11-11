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

    @GetMapping("/{movieCode:[A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9]}/poster")
    public ResponseEntity<MoviePosterResponse> getMoviePoster(
            @PathVariable String movieCode) {

        return ResponseEntity.ok(movieService.getMoviePoster(movieCode));
    }

    @PostMapping("/posters/status")
    public ResponseEntity<List<MoviePosterStatusResponse>> getPosterStatuses(
            @RequestBody(required = false) List<String> movieCodes) {

        if (movieCodes == null || movieCodes.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 중복 코드를 제거한 실제 조회 대상 수를 기준으로 제한을 검사한다.
        List<String> distinctMovieCodes = movieCodes.stream()
                .filter(code -> code != null)
                .distinct()
                .toList();

        if (distinctMovieCodes.isEmpty()
                || distinctMovieCodes.size() > MAX_STATUS_CODES
                || distinctMovieCodes.size() != movieCodes.stream().filter(code -> code != null).distinct().count()
                || distinctMovieCodes.stream().anyMatch(code ->
                        !code.matches("[A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9][A-Za-z0-9]"))) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                posterCacheService.findPosterStatuses(distinctMovieCodes)
        );
    }
}
