package com.momo.backend.client;

import com.momo.backend.service.ApiCallMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KobisApiClient {
    private final RestClient restClient;
    private final String apiKey;
    private final ApiCallMetrics metrics;

    public KobisApiClient(
            @Value("${kobis.api.base-url}") String baseUrl,
            @Value("${kobis.api.key}") String apiKey,
            ApiCallMetrics metrics) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.metrics = metrics;
    }

    public String searchMovies(String keyword) {
        return searchMovies(keyword, 1, 20);
    }

    public String searchMovies(String keyword, int page, int size) {
        return metrics.record("KOBIS", "searchMovies", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movie/searchMovieList.json")
                        .queryParam("key", apiKey).queryParam("movieNm", keyword)
                        .queryParam("curPage", page).queryParam("itemPerPage", size).build())
                .retrieve().body(String.class));
    }

    public String getMovieDetail(String movieCode) {
        return metrics.record("KOBIS", "getMovieDetail", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movie/searchMovieInfo.json")
                        .queryParam("key", apiKey).queryParam("movieCd", movieCode).build())
                .retrieve().body(String.class));
    }

    public String getDailyBoxOffice(String targetDate) {
        return metrics.record("KOBIS", "getDailyBoxOffice", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/boxoffice/searchDailyBoxOfficeList.json")
                        .queryParam("key", apiKey).queryParam("targetDt", targetDate).build())
                .retrieve().body(String.class));
    }

    public String searchMoviesByGenre(int page) {
        return metrics.record("KOBIS", "searchMoviesByGenre", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movie/searchMovieList.json")
                        .queryParam("key", apiKey).queryParam("curPage", page)
                        .queryParam("itemPerPage", 100).build())
                .retrieve().body(String.class));
    }
}