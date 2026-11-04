package com.momo.backend.dto.review;

import com.momo.backend.entity.Review;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewPageResponse {

    // 리뷰 식별 번호
    private final Long reviewId;

    // 영화 정보
    private final String movieCode;
    private final String movieTitle;
    private final Integer movieYear;

    // 리뷰 제목
    private final String title;

    // 작성자 정보
    private final Long userId;
    private final String nickname;

    // 작성일
    private final LocalDateTime createdAt;

    public ReviewPageResponse(
            Review review,
            String movieTitle,
            Integer movieYear
    ) {
        this.reviewId = review.getReviewId();

        this.movieCode = review.getMovieCode();
        this.movieTitle = movieTitle;
        this.movieYear = movieYear;

        this.title = review.getTitle();

        this.userId = review.getUser().getUserId();
        this.nickname = review.getUser().getNickname();

        this.createdAt = review.getCreatedAt();
    }
}