package com.momo.backend.dto.movie;

// 영화 검색 결과 응답 DTO
public record MovieSearchResponse(
        String movieCode,
        String title,
        String releaseDate,
        String genre,
        String director,
        String posterUrl
) {
    // 기존 5개 인자 생성 코드와의 호환성 유지
    public MovieSearchResponse(
            String movieCode,
            String title,
            String releaseDate,
            String genre,
            String director
    ) {
        this(movieCode, title, releaseDate, genre, director, "");
    }
}
