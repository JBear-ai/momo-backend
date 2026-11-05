package com.momo.backend.client;

import com.momo.backend.service.ApiCallMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmdbApiClient {
    private final RestClient restClient;
    private final String apiKey;
    private final ApiCallMetrics metrics;

    public TmdbApiClient(
            @Value("${tmdb.api.base-url}") String baseUrl,
            @Value("${tmdb.api.key}") String apiKey,
            ApiCallMetrics metrics) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.metrics = metrics;
    }

    public String searchMovie(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("영화 제목이 비어 있습니다.");
        }
        return metrics.record("TMDB", "searchMovie", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/movie")
                        .queryParam("api_key", apiKey).queryParam("query", title.trim())
                        .queryParam("language", "ko-KR").queryParam("include_adult", false).build())
                .retrieve().body(String.class));
    }

    public String getMovieDetail(long movieId) {
        if (movieId <= 0) throw new IllegalArgumentException("올바르지 않은 TMDb 영화 ID입니다.");
        return metrics.record("TMDB", "getMovieDetail", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movie/{movieId}")
                        .queryParam("api_key", apiKey).queryParam("language", "ko-KR")
                        .queryParam("append_to_response", "credits,release_dates").build(movieId))
                .retrieve().body(String.class));
    }

    public String searchTv(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("TV 프로그램 제목이 비어 있습니다.");
        }
        return metrics.record("TMDB", "searchTv", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/tv")
                        .queryParam("api_key", apiKey).queryParam("query", title.trim())
                        .queryParam("language", "ko-KR").queryParam("include_adult", false).build())
                .retrieve().body(String.class));
    }
}