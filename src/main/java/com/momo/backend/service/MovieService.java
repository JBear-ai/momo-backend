package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final KobisApiClient kobisApiClient;

    // 영화 제목으로 KOBIS 영화 목록 검색
    public String searchMovies(String keyword) {
        return kobisApiClient.searchMovies(keyword);
    }

    // 영화 코드로 상세정보 조회
    public String getMovieDetail(String movieCode) {
        return kobisApiClient.getMovieDetail(movieCode);
    }

    // 날짜별 박스오피스 조회
    public String getDailyBoxOffice(String targetDate) {
        return kobisApiClient.getDailyBoxOffice(targetDate);
    }

}