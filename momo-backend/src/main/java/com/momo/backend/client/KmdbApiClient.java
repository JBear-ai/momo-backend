package com.momo.backend.client;

import com.momo.backend.service.ApiCallMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class KmdbApiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ApiCallMetrics metrics;

    public KmdbApiClient(
            @Value("${kmdb.api.base-url}") String baseUrl,
            @Value("${kmdb.api.key}") String apiKey,
            @Value("${external-api.connect-timeout:3s}") Duration connectTimeout,
            @Value("${external-api.read-timeout:8s}") Duration readTimeout,
            ApiCallMetrics metrics) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.metrics = metrics;
    }

    public String searchMovie(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("영화 제목이 비어 있습니다.");
        }

        return metrics.record("KMDB", "searchMovie", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search_api/search_json2.jsp")
                        .queryParam("ServiceKey", apiKey)
                        .queryParam("collection", "kmdb_new2")
                        .queryParam("detail", "Y")
                        .queryParam("listCount", 100)
                        .queryParam("query", title.trim())
                        .build())
                .retrieve()
                .body(String.class));
    }
}
