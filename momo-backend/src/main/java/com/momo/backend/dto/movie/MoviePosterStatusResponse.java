package com.momo.backend.dto.movie;

import com.momo.backend.entity.PosterStatus;

// 영화 포스터 조회 상태 응답 DTO
public record MoviePosterStatusResponse(
        String movieCode,
        PosterStatus status,
        String posterUrl
) {
}