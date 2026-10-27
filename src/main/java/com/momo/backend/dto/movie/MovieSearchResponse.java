package com.momo.backend.dto.movie;

// 영화 검색 결과 응답 DTO
public record MovieSearchResponse(
        String movieCode,    // 영화 고유 코드
        String title,        // 영화 제목
        String releaseDate,  // 개봉일
        String genre,        // 장르
        String director      // 감독
) {
}