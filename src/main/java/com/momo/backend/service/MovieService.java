package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.momo.backend.dto.movie.MovieSearchResponse;
import java.util.ArrayList;
import java.util.List;
import com.momo.backend.dto.movie.MovieDetailResponse;
import java.util.stream.StreamSupport;
import com.momo.backend.dto.movie.BoxOfficeResponse;



@Service
@RequiredArgsConstructor
public class MovieService {

    private final KobisApiClient kobisApiClient;
    private final ObjectMapper objectMapper;

    // 영화 제목으로 KOBIS 영화 목록 검색
//    public String searchMovies(String keyword) {
//        return kobisApiClient.searchMovies(keyword);
//    }

    // 영화 검색 결과를 DTO 목록으로 변환
    public List<MovieSearchResponse> searchMovies(String keyword) {

        String json = kobisApiClient.searchMovies(keyword);
        List<MovieSearchResponse> movies = new ArrayList<>();

        try {
            JsonNode movieList = objectMapper.readTree(json)
                    .path("movieListResult")
                    .path("movieList");

            for (JsonNode movie : movieList) {

                String director = "";

                JsonNode directors = movie.path("directors");
                if (directors.isArray() && !directors.isEmpty()) {
                    director = directors.get(0).path("peopleNm").asText("");
                }

                movies.add(new MovieSearchResponse(
                        movie.path("movieCd").asText(""),
                        movie.path("movieNm").asText(""),
                        movie.path("openDt").asText(""),
                        movie.path("genreAlt").asText(""),
                        director
                ));
            }

        } catch (Exception e) {
            throw new IllegalStateException("영화 검색 결과를 처리하지 못했습니다.", e);
        }

        return movies;
    }


    // 영화 코드로 상세정보 조회
//    public String getMovieDetail(String movieCode) {
//        return kobisApiClient.getMovieDetail(movieCode);
//    }

    // 영화 상세정보를 DTO로 변환
    public MovieDetailResponse getMovieDetail(String movieCode) {

        String json = kobisApiClient.getMovieDetail(movieCode);

        try {
            JsonNode movie = objectMapper.readTree(json)
                    .path("movieInfoResult")
                    .path("movieInfo");

            // 감독 정보
            String director = "";
            JsonNode directors = movie.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {
                director = directors.get(0).path("peopleNm").asText("");
            }

            // 장르 정보
            String genre = StreamSupport.stream(
                            movie.path("genres").spliterator(), false)
                    .map(node -> node.path("genreNm").asText(""))
                    .filter(name -> !name.isBlank())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            // 출연 배우 목록
            List<String> actors = new ArrayList<>();

            for (JsonNode actor : movie.path("actors")) {
                String name = actor.path("peopleNm").asText("");

                if (!name.isBlank()) {
                    actors.add(name);
                }
                if (actors.size() >=10) {
                    break;
                    // 출연 배우는 최대 10명까지만 저장
                }
            }

            // 관람등급
            String watchGrade = "";
            JsonNode audits = movie.path("audits");

            if (audits.isArray() && !audits.isEmpty()) {
                watchGrade = audits.get(0).path("watchGradeNm").asText("");
            }

            return new MovieDetailResponse(
                    movie.path("movieCd").asText(""),
                    movie.path("movieNm").asText(""),
                    movie.path("movieNmEn").asText(""),
                    movie.path("openDt").asText(""),
                    movie.path("showTm").asText(""),
                    genre,
                    director,
                    actors,
                    watchGrade
            );

        } catch (Exception e) {
            throw new IllegalStateException("영화 상세정보를 처리하지 못했습니다.", e);
        }
    }

    // 날짜별 박스오피스 조회
//    public String getDailyBoxOffice(String targetDate) {
//        return kobisApiClient.getDailyBoxOffice(targetDate);
//    }
    // 일별 박스오피스 결과를 DTO 목록으로 변환
    public List<BoxOfficeResponse> getDailyBoxOffice(String targetDate) {

        String json = kobisApiClient.getDailyBoxOffice(targetDate);
        List<BoxOfficeResponse> movies = new ArrayList<>();

        try {
            JsonNode boxOfficeList = objectMapper.readTree(json)
                    .path("boxOfficeResult")
                    .path("dailyBoxOfficeList");

            for (JsonNode movie : boxOfficeList) {

                movies.add(new BoxOfficeResponse(
                        movie.path("rank").asInt(),
                        movie.path("movieCd").asText(""),
                        movie.path("movieNm").asText(""),
                        movie.path("openDt").asText(""),
                        movie.path("audiCnt").asText(""),
                        movie.path("audiAcc").asText("")
                ));
            }

        } catch (Exception e) {
            throw new IllegalStateException("박스오피스 결과를 처리하지 못했습니다.", e);
        }

        return movies;
    }

}