package com.momo.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KobisApiClient {

    private final RestClient restClient;
    private final String apiKey;

    // KOBIS API 접속 주소와 인증 키 설정
    public KobisApiClient(
            @Value("${kobis.api.base-url}") String baseUrl,
            @Value("${kobis.api.key}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }

    // 영화 검색 API 호출
    public String searchMovies(String keyword) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/searchMovieList.json")
                        .queryParam("key", apiKey)
                        .queryParam("movieNm", keyword)
                        .build())
                .retrieve()
                .body(String.class);
    }

    // 영화 상세 정보 API 호출
    public String getMovieDetail(String movieCode) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/searchMovieInfo.json")
                        .queryParam("key", apiKey)
                        .queryParam("movieCd", movieCode)
                        .build())
                .retrieve()
                .body(String.class);
    }

    // 일별 박스오피스 API 호출
    public String getDailyBoxOffice(String targetDate) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/boxoffice/searchDailyBoxOfficeList.json")
                        .queryParam("key", apiKey)
                        .queryParam("targetDt", targetDate)
                        .build())
                .retrieve()
                .body(String.class);
    }
}