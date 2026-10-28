
package com.momo.backend.controller;

import com.momo.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class MovieRatingController {

    // 영화 평가 데이터를 조회하기 위한 Service
    private final ReviewService reviewService;

    // 특정 영화의 좋아요/싫어요 개수와 긍정 평가 비율 조회
    @GetMapping("/{movieCode}/rating")
    public ResponseEntity<Map<String, Object>> getMovieRating(
            @PathVariable String movieCode) {

        // Service에서 영화 평가 결과 조회
        Map<String, Object> result = reviewService.getMovieRating(movieCode);

        // 조회 결과 반환
        return ResponseEntity.ok(result);
    }
}
