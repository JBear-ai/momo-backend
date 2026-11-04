package com.momo.backend.controller;

import com.momo.backend.dto.review.ReviewCreateRequest;
import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.dto.review.ReviewPageResponse;
import com.momo.backend.dto.review.ReviewUpdateRequest;

import com.momo.backend.service.ReviewService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    // 리뷰 관련 비즈니스 로직을 처리하는 Service
    private final ReviewService reviewService;


    // =========================================================
    // 1. 전체 리뷰 목록 조회
    // =========================================================

    @GetMapping
    public ResponseEntity<Page<ReviewPageResponse>> getAllReviews(
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                reviewService.getAllReviews(page)
        );
    }


    // =========================================================
    // 2. 리뷰 작성
    // =========================================================

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewCreateRequest dto,
            Authentication authentication) {

        ReviewResponse response = reviewService.createReview(
                authentication.getName(),
                dto
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // 3. 리뷰 상세 조회
    // =========================================================

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
            @PathVariable Long reviewId) {

        return ResponseEntity.ok(
                reviewService.getReview(reviewId)
        );
    }


    // =========================================================
    // 4. 내가 작성한 리뷰 목록 조회
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                reviewService.getMyReviews(
                        authentication.getName(),
                        page
                )
        );
    }


    // =========================================================
    // 5. 특정 영화의 리뷰 목록 조회
    // =========================================================

    @GetMapping("/movie/{movieCode}")
    public ResponseEntity<Page<ReviewResponse>> getMovieReviews(
            @PathVariable String movieCode,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                reviewService.getMovieReviews(
                        movieCode,
                        page
                )
        );
    }


    // =========================================================
    // 6. 특정 영화의 좋아요 / 싫어요 통계
    // =========================================================

    @GetMapping("/movie/{movieCode}/rating")
    public ResponseEntity<Map<String, Object>> getMovieRating(
            @PathVariable String movieCode) {

        return ResponseEntity.ok(
                reviewService.getMovieRating(movieCode)
        );
    }


    // =========================================================
    // 7. 리뷰 수정
    // =========================================================

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest dto,
            Authentication authentication) {

        ReviewResponse response = reviewService.updateReview(
                reviewId,
                authentication.getName(),
                dto
        );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // 8. 리뷰 삭제
    // =========================================================

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {

        reviewService.deleteReview(
                reviewId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

}