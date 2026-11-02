
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

    // TMDB 영화 검색
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

    // TMDB TV 프로그램 검색
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
