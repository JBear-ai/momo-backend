package com.momo.backend.dto.movie;

// 일별 박스오피스 응답 DTO
public record BoxOfficeResponse(
        int rank,              // 박스오피스 순위
        String movieCode,      // 영화 고유 코드
        String title,          // 영화 제목
        String releaseDate,    // 개봉일
        String audienceCount,  // 당일 관객 수
        String totalAudience   // 누적 관객 수
) {
}