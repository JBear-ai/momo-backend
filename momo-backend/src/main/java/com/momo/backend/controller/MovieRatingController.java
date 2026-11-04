
package com.momo.backend.controller;

import com.momo.backend.service.MovieRatingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieRatingController {

    private final MovieRatingService movieRatingService;

    // 특정 영화의 전체 평가 및 로그인 회원의 평가 상태 조회
    @GetMapping("/{movieCode}/rating")
    public ResponseEntity<Map<String, Object>> getMovieRating(
            @PathVariable String movieCode,
            Authentication authentication
    ) {

        String loginId = authentication != null
                ? authentication.getName()
                : null;

        Map<String, Object> result =
                movieRatingService.getMovieRating(
                        movieCode,
                        loginId
                );

        return ResponseEntity.ok(result);
    }

    // 영화 좋아요 또는 싫어요 등록 및 변경
    @PutMapping("/{movieCode}/rating")
    public ResponseEntity<Void> saveMovieRating(
            @PathVariable String movieCode,
            @RequestBody Boolean liked,
            Authentication authentication
    ) {

        movieRatingService.saveRating(
                authentication.getName(),
                movieCode,
                liked
        );

        return ResponseEntity.ok().build();
    }

    // 영화 평가 취소
    @DeleteMapping("/{movieCode}/rating")
    public ResponseEntity<Void> deleteMovieRating(
            @PathVariable String movieCode,
            Authentication authentication
    ) {

        movieRatingService.deleteRating(
                authentication.getName(),
                movieCode
        );

        return ResponseEntity.noContent().build();
    }
}
