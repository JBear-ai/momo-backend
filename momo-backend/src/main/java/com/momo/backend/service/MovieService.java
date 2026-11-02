
package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import com.momo.backend.dto.movie.BoxOfficeResponse;
import com.momo.backend.dto.movie.MovieDetailResponse;
import com.momo.backend.dto.movie.MoviePosterResponse;
import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.dto.movie.MovieSearchPageResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.PosterStatus;
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

    private final MoviePosterAsyncService posterAsyncService;
    private final KobisApiClient kobisApiClient;
    private final KmdbService kmdbService;
    private final ObjectMapper objectMapper;
    private final MovieRepository movieRepository;
    private final MoviePosterCacheService posterCacheService;

    // =========================================================
    // 1. 영화 검색 (페이지네이션)
    // =========================================================

    public MovieSearchPageResponse searchMovies(
            String keyword,
            int page,
            int size) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        if (page < 1 || size < 1) {
            throw new IllegalArgumentException(
                    "페이지와 검색 개수는 1 이상이어야 합니다."
            );
        }

        String json = kobisApiClient.searchMovies(
                keyword.trim(),
                page,
                size
        );

        List<MovieSearchResponse> movies = new ArrayList<>();

        int totalCount = 0;

        try {

            JsonNode root = objectMapper.readTree(json);

            JsonNode movieListResult = root.path("movieListResult");

            totalCount = movieListResult
                    .path("totCnt")
                    .asInt(0);

            JsonNode movieList = movieListResult.path("movieList");

            if (movieList.isArray()) {

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
            }

        } catch (Exception e) {

            throw new IllegalStateException(
                    "영화 검색 결과를 처리하지 못했습니다.",
                    e
            );
        }

        boolean hasNext = (long) page * size < totalCount;

        return new MovieSearchPageResponse(
                movies,
                page,
                size,
                totalCount,
                hasNext
        );
    }

    // =========================================================
    // 2. 영화 상세정보 조회
    // =========================================================

    public MovieDetailResponse getMovieDetail(String movieCode) {

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

        String json = kobisApiClient.getMovieDetail(movieCode);

        try {

            JsonNode movie = objectMapper.readTree(json)
                    .path("movieInfoResult")
                    .path("movieInfo");

            String director = "";

            JsonNode directors = movie.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {
                director = directors.get(0)
                        .path("peopleNm")
                        .asText("");
            }

            String genre = StreamSupport.stream(
                            movie.path("genres").spliterator(),
                            false
                    )
                    .map(node -> node.path("genreNm").asText(""))
                    .filter(name -> !name.isBlank())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

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

            String watchGrade = "";

            JsonNode audits = movie.path("audits");

            if (audits.isArray() && !audits.isEmpty()) {
                watchGrade = audits.get(0)
                        .path("watchGradeNm")
                        .asText("");
            }

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

                String posterUrl = kmdbService.findPosterUrl(
                        movieCode,
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
    // 4. 영화 코드로 포스터 조회 및 비동기 캐시 관리
    // =========================================================

    public MoviePosterResponse getMoviePoster(String movieCode) {

        // 1. 기존 포스터 캐시 조회
        MoviePosterCache savedCache = posterCacheService
                .findCache(movieCode)
                .orElse(null);

        // 2. 이미 조회에 성공한 포스터라면 캐시 주소 반환
        if (savedCache != null) {

            if (savedCache.getStatus() == PosterStatus.SUCCESS) {

                return new MoviePosterResponse(
                        movieCode,
                        savedCache.getPosterUrl() == null
                                ? ""
                                : savedCache.getPosterUrl()
                );
            }

            // 이미 조회 중이거나 실패한 경우 빈 주소 반환
            return new MoviePosterResponse(movieCode, "");
        }

        // 3. 캐시가 없다면 영화 상세정보 조회
        MovieDetailResponse movie = getMovieDetail(movieCode);

        // 4. 포스터 캐시 생성
        posterCacheService.createCache(
                movieCode,
                movie.title(),
                movie.releaseDate()
        );

        // 5. 포스터 조회 상태 변경
        posterCacheService.markAsProcessing(movieCode);

        // 6. 백그라운드 포스터 검색 실행
        posterAsyncService.searchPosterAsync(
                movieCode,
                movie.title(),
                movie.releaseDate()
        );

        // 7. 포스터 검색 완료를 기다리지 않고 즉시 반환
        return new MoviePosterResponse(movieCode, "");
    }
}
