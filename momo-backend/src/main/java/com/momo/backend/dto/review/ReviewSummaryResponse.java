package com.momo.backend.dto.review;

public record ReviewSummaryResponse(
        Long reviewId,
        String movieCode,
        String movieTitle,
        String releaseDate,
        long commentCount,
        long likeCount,
        long dislikeCount
) {
}
