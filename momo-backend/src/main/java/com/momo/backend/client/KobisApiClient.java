package com.momo.backend.client;

import com.momo.backend.service.ApiCallMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class KobisApiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ApiCallMetrics metrics;
    private final com.momo.backend.service.ApiCallBudgetService apiCallBudgetService;

    public KobisApiClient(
            @Value("${kobis.api.base-url}") String baseUrl,
            @Value("${kobis.api.key}") String apiKey,
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

    public String searchMovies(String keyword) {
        return searchMovies(keyword, 1, 20);
    }

    public String searchMovies(String keyword, int page, int size) {
        return metrics.record("KOBIS", "searchMovies", () -> ExternalApiRetry.execute(() -> {
            // 재시도도 실제 외부 요청이므로 각 시도마다 API 예산을 차감한다.
            apiCallBudgetService.acquire(
                    "KOBIS", com.momo.backend.service.ApiCallBudgetService.TrafficType.USER_SEARCH);
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/movie/searchMovieList.json")
                            .queryParam("key", apiKey)
                            .queryParam("movieNm", keyword)
                            .queryParam("curPage", page)
                            .queryParam("itemPerPage", size)
                            .build())
                    .retrieve()
                    .body(String.class);
        }));
    }

    /**
     * 검색어 없이 KOBIS 영화 목록 전체를 페이지 단위로 조회한다.
     * 최근 동기화에서는 개봉연도 범위를 지정한다.
     */
    public String searchCatalogMovies(int page, int size, Integer startYear, Integer endYear) {
        return searchCatalogMovies(page, size, startYear, endYear,
                com.momo.backend.service.ApiCallBudgetService.TrafficType.PREFETCH);
    }

    private String searchCatalogMovies(
            int page, int size, Integer startYear, Integer endYear,
            com.momo.backend.service.ApiCallBudgetService.TrafficType trafficType) {
        return metrics.record("KOBIS", "searchCatalogMovies", () -> ExternalApiRetry.execute(() -> {
            apiCallBudgetService.acquire("KOBIS", trafficType);
            return restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/movie/searchMovieList.json")
                                .queryParam("key", apiKey)
                                .queryParam("curPage", page)
                                .queryParam("itemPerPage", size);
                        if (startYear != null) {
                            builder.queryParam("openStartDt", startYear);
                        }
                        if (endYear != null) {
                            builder.queryParam("openEndDt", endYear);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(String.class);
        }));
    }

    public String getMovieDetail(String movieCode) {
        return metrics.record("KOBIS", "getMovieDetail", () -> ExternalApiRetry.execute(() -> {
            apiCallBudgetService.acquire(
                    "KOBIS", com.momo.backend.service.ApiCallBudgetService.TrafficType.USER_SEARCH);
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/movie/searchMovieInfo.json")
                            .queryParam("key", apiKey)
                            .queryParam("movieCd", movieCode)
                            .build())
                    .retrieve()
                    .body(String.class);
        }));
    }

    public String getDailyBoxOffice(String targetDate) {
        return getDailyBoxOffice(targetDate,
                com.momo.backend.service.ApiCallBudgetService.TrafficType.USER_SEARCH);
    }

    // 이력 사전 수집은 PREFETCH 예산을 사용해 사용자 검색에 예약된 호출량을 보호한다.
    public String getDailyBoxOffice(String targetDate,
            com.momo.backend.service.ApiCallBudgetService.TrafficType trafficType) {
        return metrics.record("KOBIS", "getDailyBoxOffice", () -> ExternalApiRetry.execute(() -> {
            apiCallBudgetService.acquire("KOBIS", trafficType);
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/boxoffice/searchDailyBoxOfficeList.json")
                            .queryParam("key", apiKey)
                            .queryParam("targetDt", targetDate)
                            .build())
                    .retrieve()
                    .body(String.class);
        }));
    }

    public String getWeeklyBoxOffice(String targetDate) {
        return metrics.record("KOBIS", "getWeeklyBoxOffice", () -> ExternalApiRetry.execute(() -> {
            apiCallBudgetService.acquire(
                    "KOBIS", com.momo.backend.service.ApiCallBudgetService.TrafficType.PREFETCH);
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/boxoffice/searchWeeklyBoxOfficeList.json")
                            .queryParam("key", apiKey)
                            .queryParam("targetDt", targetDate)
                            .queryParam("weekGb", 0)
                            .build())
                    .retrieve()
                    .body(String.class);
        }));
    }

    public String searchMoviesByGenre(int page) {
        return searchCatalogMovies(page, 100, null, null,
                com.momo.backend.service.ApiCallBudgetService.TrafficType.USER_SEARCH);
    }
}
