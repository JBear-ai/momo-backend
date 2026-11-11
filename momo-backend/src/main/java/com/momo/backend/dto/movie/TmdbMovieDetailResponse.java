package com.momo.backend.dto.movie;

import java.util.List;
import java.util.Map;

// KOBIS 코드가 아직 없는 TMDb 영화 상세정보 응답
public record TmdbMovieDetailResponse(
        long tmdbId,
        String movieCode,
        String title,
        String titleEn,
        String releaseDate,
        String runningTime,
        String genre,
        String director,
        List<String> actors,
        String watchGrade,
        String overview,
        String posterUrl,
        List<Map<String, Object>> people
) {
}
