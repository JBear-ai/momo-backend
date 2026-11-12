package com.momo.backend.dto.review;

public record ReviewSummaryResponse(
        Long reviewId,
        long commentCount,
        long likeCount,
        long dislikeCount
) {
}
