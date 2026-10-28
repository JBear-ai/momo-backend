package com.momo.backend.dto.review;

import com.momo.backend.entity.Review;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewResponse {

    // 리뷰 식별 번호
    private final Long reviewId;

    // 작성자 정보
    private final Long userId;
    private final String nickname;

    // 리뷰가 작성된 영화 코드
    private final String movieCode;

    // 리뷰 제목, 내용, 영화 평가
    private final String title;
    private final String content;
    private final Boolean liked;

    // 리뷰 작성 및 수정 시간
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // Review Entity를 응답 DTO로 변환
    public ReviewResponse(Review review) {
        this.reviewId = review.getReviewId();
        this.userId = review.getUser().getUserId();
        this.nickname = review.getUser().getNickname();
        this.movieCode = review.getMovieCode();
        this.title = review.getTitle();
        this.content = review.getContent();
        this.liked = review.getLiked();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
    }
}