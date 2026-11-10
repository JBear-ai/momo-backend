package com.momo.backend.client;

import com.momo.backend.service.ApiCallMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class TmdbApiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ApiCallMetrics metrics;
    private final com.momo.backend.service.ApiCallBudgetService apiCallBudgetService;

    public TmdbApiClient(
            @Value("${tmdb.api.base-url}") String baseUrl,
            @Value("${tmdb.api.key}") String apiKey,
            @Value("${external-api.connect-timeout:3s}") Duration connectTimeout,
            @Value("${external-api.read-timeout:8s}") Duration readTimeout,
            ApiCallMetrics metrics,
            com.momo.backend.service.ApiCallBudgetService apiCallBudgetService) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.metrics = metrics;
        this.apiCallBudgetService = apiCallBudgetService;
    }

    public String searchMovie(String title) {
        return searchMovie(title, "ko-KR");
    }

    // 검색어 언어에 맞춰 TMDb 결과 제목을 조회한다.
    public String searchMovie(String title, String language) {
        apiCallBudgetService.acquire("TMDB", com.momo.backend.service.ApiCallBudgetService.TrafficType.PREFETCH);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("영화 제목이 비어 있습니다.");
        }

        String safeLanguage = (language == null || language.isBlank())
                ? "ko-KR"
                : language;

        return metrics.record("TMDB", "searchMovie", () -> ExternalApiRetry.execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", title.trim())
                        .queryParam("language", safeLanguage)
                        .queryParam("include_adult", false)
                        .build())
                .retrieve()
                .body(String.class)));
    }

    public String getMovieDetail(long movieId) {
        apiCallBudgetService.acquire("TMDB", com.momo.backend.service.ApiCallBudgetService.TrafficType.PREFETCH);
        if (movieId <= 0) {
            throw new IllegalArgumentException("올바르지 않은 TMDb 영화 ID입니다.");
        }

        return metrics.record("TMDB", "getMovieDetail", () -> ExternalApiRetry.execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movie/{movieId}")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "ko-KR")
                        .queryParam("append_to_response", "credits,release_dates")
                        .build(movieId))
                .retrieve()
                .body(String.class)));
    }

    public String searchTv(String title) {
        apiCallBudgetService.acquire("TMDB", com.momo.backend.service.ApiCallBudgetService.TrafficType.PREFETCH);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("TV 프로그램 제목이 비어 있습니다.");
        }

        return metrics.record("TMDB", "searchTv", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/tv")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", title.trim())
                        .queryParam("language", "ko-KR")
                        .queryParam("include_adult", false)
                        .build())
                .retrieve()
                .body(String.class));
    }

    public String searchPerson(String name, String language) {
        apiCallBudgetService.acquire("TMDB", com.momo.backend.service.ApiCallBudgetService.TrafficType.PREFETCH);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("인물 이름이 비어 있습니다.");
        }

        String safeLanguage = (language == null || language.isBlank())
                ? "ko-KR"
                : language;

        return metrics.record("TMDB", "searchPerson", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/person")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", name.trim())
                        .queryParam("language", safeLanguage)
                        .queryParam("include_adult", false)
                        .build())
                .retrieve()
                .body(String.class));
    }

    public String getPersonDetail(long personId, String language) {
        apiCallBudgetService.acquire("TMDB", com.momo.backend.service.ApiCallBudgetService.TrafficType.PREFETCH);
        if (personId <= 0) {
            throw new IllegalArgumentException("올바르지 않은 TMDb 인물 ID입니다.");
        }

        String safeLanguage = (language == null || language.isBlank())
                ? "ko-KR"
                : language;

        return metrics.record("TMDB", "getPersonDetail", () -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/person/{personId}")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", safeLanguage)
                        .queryParam("append_to_response", "combined_credits")
                        .build(personId))
                .retrieve()
                .body(String.class));
    }
}
