
package com.momo.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KmdbApiClient {

    private final RestClient restClient;
    private final String apiKey;

    public KmdbApiClient(
            @Value("${kmdb.api.base-url}") String baseUrl,
            @Value("${kmdb.api.key}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }

    /**
     * 영화 제목으로 KMDb 영화 정보 검색
     */
    public String searchMovie(String title) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("영화 제목이 비어 있습니다.");
        }

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search_api/search_json2.jsp")
                        .queryParam("ServiceKey", apiKey)
                        .queryParam("collection", "kmdb_new2")
                        .queryParam("detail", "Y")
                        .queryParam("listCount", 100)
                        .queryParam("query", title.trim())
                        .build())
                .retrieve()
                .body(String.class);
    }
}
