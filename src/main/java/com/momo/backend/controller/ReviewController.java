package com.momo.backend.controller;

import com.momo.backend.dto.review.ReviewCreateRequest;
import com.momo.backend.dto.review.ReviewResponse;
import com.momo.backend.service.ReviewService;
import com.momo.backend.dto.review.ReviewUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins="http://localhost:3000")
@Slf4j
public class ReviewController {

    // 리뷰 관련 비즈니스 로직을 처리하는 Service
    private final ReviewService reviewService;

    // 로그인한 사용자의 리뷰 작성 요청 처리
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewCreateRequest dto,
            Authentication authentication) {

        // 세션에 저장된 로그인 아이디를 작성자로 사용
        ReviewResponse response = reviewService.createReview(
                authentication.getName(),
                dto
        );

        // 리뷰 생성 성공 시 HTTP 201 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 리뷰 ID를 이용한 상세 조회
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
            @PathVariable Long reviewId) {

        return ResponseEntity.ok(
                reviewService.getReview(reviewId)
        );
    }

    // 로그인한 사용자의 리뷰 수정 요청
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

    // 로그인한 사용자의 리뷰 삭제 요청
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