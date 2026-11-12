package com.momo.backend.controller;

import com.momo.backend.dto.movie.MoviePosterResponse;
import com.momo.backend.dto.movie.MoviePosterStatusResponse;
import com.momo.backend.service.MoviePosterCacheService;
import com.momo.backend.service.MovieService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
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

    // 리뷰 목록 등에서 여러 영화의 포스터 수집을 한 번에 등록한다.
    @PostMapping("/posters/ensure")
    public ResponseEntity<List<MoviePosterStatusResponse>> ensurePosterCollection(
            @RequestBody(required = false) List<String> movieCodes) {

        List<String> distinctMovieCodes = validateMovieCodes(movieCodes);
        if (distinctMovieCodes == null) {
            return ResponseEntity.badRequest().build();
        }

        for (String movieCode : distinctMovieCodes) {
            try {
                movieService.getMoviePoster(movieCode);
            } catch (Exception exception) {
                // 한 영화의 외부 정보 또는 포스터 조회 실패가 나머지 영화 처리를 중단시키지 않는다.
                log.warn("일괄 포스터 수집 등록 실패. movieCode={}", movieCode, exception);
            }
        }

        return ResponseEntity.ok(posterCacheService.findPosterStatuses(distinctMovieCodes));
    }

    @PostMapping("/posters/status")
    public ResponseEntity<List<MoviePosterStatusResponse>> getPosterStatuses(
            @RequestBody(required = false) List<String> movieCodes) {

        List<String> distinctMovieCodes = validateMovieCodes(movieCodes);
        if (distinctMovieCodes == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                posterCacheService.findPosterStatuses(distinctMovieCodes)
        );
    }

    private List<String> validateMovieCodes(List<String> movieCodes) {
        if (movieCodes == null || movieCodes.isEmpty()) return null;

        // 잘못된 코드가 섞인 요청은 거부하고, 중복 제거 후 실제 조회 수를 제한한다.
        if (movieCodes.stream().anyMatch(code ->
                code == null || !code.matches("[A-Za-z0-9]{8}"))) {
            return null;
        }

        List<String> distinctMovieCodes = movieCodes.stream()
                .distinct()
                .toList();

        return distinctMovieCodes.size() > MAX_STATUS_CODES
                ? null
                : distinctMovieCodes;
    }
}
