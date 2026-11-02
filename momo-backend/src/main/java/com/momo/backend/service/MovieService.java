
package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import com.momo.backend.dto.movie.BoxOfficeResponse;
import com.momo.backend.dto.movie.MovieDetailResponse;
import com.momo.backend.dto.movie.MoviePosterResponse;
import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final KobisApiClient kobisApiClient;
    private final KmdbService kmdbService;
    private final ObjectMapper objectMapper;
    private final MovieRepository movieRepository;

    // =========================================================
    // 1. 영화 검색
    // =========================================================

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
                    director = directors.get(0)
                            .path("peopleNm")
                            .asText("");
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

            throw new IllegalStateException(
                    "영화 검색 결과를 처리하지 못했습니다.",
                    e
            );
        }

        return movies;
    }

    // =========================================================
    // 2. 영화 상세정보 조회
    // =========================================================

    public MovieDetailResponse getMovieDetail(String movieCode) {

        // DB에 저장된 영화 정보가 있는지 먼저 확인
        Movie savedMovie = movieRepository.findById(movieCode)
                .orElse(null);

        if (savedMovie != null) {

            List<String> actors = new ArrayList<>();

            if (savedMovie.getActors() != null
                    && !savedMovie.getActors().isBlank()) {

                actors = Arrays.asList(
                        savedMovie.getActors().split(",")
                );
            }

            return new MovieDetailResponse(
                    savedMovie.getMovieCode(),
                    savedMovie.getTitle(),
                    savedMovie.getTitleEn(),
                    savedMovie.getReleaseDate(),
                    savedMovie.getShowTime(),
                    savedMovie.getGenre(),
                    savedMovie.getDirector(),
                    actors,
                    savedMovie.getWatchGrade()
            );
        }

        // DB에 영화 정보가 없을 때만 KOBIS API 호출
        String json = kobisApiClient.getMovieDetail(movieCode);

        try {

            JsonNode movie = objectMapper.readTree(json)
                    .path("movieInfoResult")
                    .path("movieInfo");

            // 감독 정보
            String director = "";

            JsonNode directors = movie.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {
                director = directors.get(0)
                        .path("peopleNm")
                        .asText("");
            }

            // 장르 정보
            String genre = StreamSupport.stream(
                            movie.path("genres").spliterator(),
                            false
                    )
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

                if (actors.size() >= 10) {
                    break;
                }
            }

            // 관람등급
            String watchGrade = "";

            JsonNode audits = movie.path("audits");

            if (audits.isArray() && !audits.isEmpty()) {
                watchGrade = audits.get(0)
                        .path("watchGradeNm")
                        .asText("");
            }

            // KOBIS 영화 정보 DB 저장
            Movie newMovie = new Movie(
                    movie.path("movieCd").asText(""),
                    movie.path("movieNm").asText(""),
                    movie.path("movieNmEn").asText(""),
                    movie.path("openDt").asText(""),
                    movie.path("showTm").asText(""),
                    genre,
                    director,
                    String.join(",", actors),
                    watchGrade
            );

            movieRepository.save(newMovie);

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

            throw new IllegalStateException(
                    "영화 상세정보를 처리하지 못했습니다.",
                    e
            );
        }
    }

    // =========================================================
    // 3. 일별 박스오피스 조회
    // =========================================================

    public List<BoxOfficeResponse> getDailyBoxOffice(String targetDate) {

        String json = kobisApiClient.getDailyBoxOffice(targetDate);

        List<BoxOfficeResponse> movies = new ArrayList<>();

        try {

            JsonNode boxOfficeList = objectMapper.readTree(json)
                    .path("boxOfficeResult")
                    .path("dailyBoxOfficeList");

            for (JsonNode movie : boxOfficeList) {

                String movieCode = movie.path("movieCd").asText("");
                String title = movie.path("movieNm").asText("");
                String releaseDate = movie.path("openDt").asText("");

                // 공통 KMDb 서비스로 포스터 검색
                String posterUrl = kmdbService.findPosterUrl(
                        title,
                        releaseDate
                );

                movies.add(new BoxOfficeResponse(
                        movie.path("rank").asInt(),
                        movieCode,
                        title,
                        releaseDate,
                        movie.path("audiCnt").asText(""),
                        movie.path("audiAcc").asText(""),
                        posterUrl == null ? "" : posterUrl
                ));
            }

        } catch (Exception e) {

            throw new IllegalStateException(
                    "박스오피스 결과를 처리하지 못했습니다.",
                    e
            );
        }

        return movies;
    }

    // =========================================================
    // 4. 영화 코드로 포스터 조회
    // =========================================================

    public MoviePosterResponse getMoviePoster(String movieCode) {

        // 영화 상세정보 조회
        // DB에 없으면 KOBIS API에서 조회하고 저장
        MovieDetailResponse movie = getMovieDetail(movieCode);

        // 박스오피스와 동일한 공통 검색 메서드 사용
        String posterUrl = kmdbService.findPosterUrl(
                movie.title(),
                movie.releaseDate()
        );

        return new MoviePosterResponse(
                movieCode,
                posterUrl
        );
    }
}
