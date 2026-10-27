package com.momo.backend.dto.movie;

import java.util.List;

// 영화 상세정보 응답 DTO
public record MovieDetailResponse(
        String movieCode,       // 영화 고유 코드
        String title,           // 영화 제목
        String titleEn,         // 영문 제목
        String releaseDate,     // 개봉일
        String runningTime,     // 상영시간(분)
        String genre,           // 장르
        String director,        // 감독
        List<String> actors,    // 출연 배우
        String watchGrade       // 관람등급
) {
}