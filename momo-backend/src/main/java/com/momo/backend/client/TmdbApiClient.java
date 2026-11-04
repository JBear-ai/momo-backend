
package com.momo.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmdbApiClient {

    private final RestClient restClient;
    private final String apiKey;

    public TmdbApiClient(
            @Value("${tmdb.api.base-url}") String baseUrl,
            @Value("${tmdb.api.key}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }

    // =========================================================
    // 1. TMDb 영화 검색
    // =========================================================

    public String searchMovie(String title) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("영화 제목이 비어 있습니다.");
        }

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", title.trim())
                        .queryParam("language", "ko-KR")
                        .queryParam("include_adult", false)
                        .build())
                .retrieve()
                .body(String.class);
    }

    // =========================================================
    // 2. TMDb 영화 상세정보 조회
    // 영화 상세정보 + 감독 + 배우 + 국가별 개봉일
    // =========================================================

    public String getMovieDetail(long movieId) {

        if (movieId <= 0) {
            throw new IllegalArgumentException("올바르지 않은 TMDb 영화 ID입니다.");
        }

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{movieId}")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "ko-KR")
                        .queryParam("append_to_response", "credits,release_dates")
                        .build(movieId))
                .retrieve()
                .body(String.class);
    }

    // =========================================================
    // 3. TMDb TV 프로그램 검색
    // =========================================================

    public String searchTv(String title) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("TV 프로그램 제목이 비어 있습니다.");
        }

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/tv")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", title.trim())
                        .queryParam("language", "ko-KR")
                        .queryParam("include_adult", false)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
