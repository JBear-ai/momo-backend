
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

    // 영화 검색 API 호출 (기본 검색)
    public String searchMovies(String keyword) {

        return searchMovies(keyword, 1, 20);
    }

    // 영화 검색 API 호출 (페이지네이션)
    public String searchMovies(String keyword, int page, int size) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/searchMovieList.json")
                        .queryParam("key", apiKey)
                        .queryParam("movieNm", keyword)
                        .queryParam("curPage", page)
                        .queryParam("itemPerPage", size)
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

    // 장르별 영화 목록 검색 API 호출
    public String searchMoviesByGenre(int page) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/searchMovieList.json")
                        .queryParam("key", apiKey)
                        .queryParam("curPage", page)
                        .queryParam("itemPerPage", 100)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
