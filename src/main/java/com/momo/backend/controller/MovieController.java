package com.momo.backend.controller;

import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.service.ReviewService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    // 영화별 리뷰 조회 기능을 처리하는 Service
    private final ReviewService reviewService;

    // 특정 영화에 작성된 리뷰 목록 조회
    @GetMapping("/{movieCode}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getMovieReviews(
            @PathVariable String movieCode,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                reviewService.getMovieReviews(movieCode, page)
        );
    }
}