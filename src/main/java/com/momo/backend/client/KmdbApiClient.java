
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

    // 영화 제목으로 KMDb 영화 정보 검색
    public String searchMovie(String title) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search_api/search_json2.jsp")
                        .queryParam("ServiceKey", apiKey)
                        .queryParam("collection", "kmdb_new2")
                        .queryParam("detail", "Y")
                        .queryParam("query", title)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
