
package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import com.momo.backend.dto.movie.BoxOfficeResponse;
import com.momo.backend.dto.movie.MovieDetailResponse;
import com.momo.backend.dto.movie.MoviePosterResponse;
import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.dto.movie.MovieSearchPageResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.MovieSupplement;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
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
    private final TmdbService tmdbService;
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

        // 검색어 앞뒤 공백 및 중복 공백 제거
        String[] words = keyword.trim().split("\\s+");

        List<String> keywords = new ArrayList<>();

        // 중복 검색 단어 제거
        for (String word : words) {
            if (!keywords.contains(word)) {
                keywords.add(word);
            }
        }

        String searchKeyword = String.join(" ", keywords);

        // 정리된 검색어로 KOBIS API 호출
        String json = kobisApiClient.searchMovies(
                searchKeyword,
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

        // 검색 결과 전체의 포스터 수집 작업 등록
        enqueuePosterSearches(movies);

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
    // 2. 검색 및 장르별 영화의 포스터 비동기 등록
    // =========================================================

    private void enqueuePosterSearches(
            List<MovieSearchResponse> movies) {

        for (MovieSearchResponse movie : movies) {

            String movieCode = movie.movieCode();
            String title = movie.title();
            String releaseDate = movie.releaseDate();

            // 영화 코드나 제목이 없는 데이터는 제외
            if (movieCode == null || movieCode.isBlank()
                    || title == null || title.isBlank()) {
                continue;
            }

            try {

                // 검색 결과에 포함된 정보로 포스터 캐시 생성
                posterCacheService.createCache(
                        movieCode,
                        title,
                        releaseDate
                );

                // PENDING 상태인 경우에만 검색 작업 시작
                boolean started = posterCacheService
                        .tryMarkAsProcessing(movieCode);

                if (!started) {
                    continue;
                }

                // 백그라운드 포스터 검색 실행
                posterAsyncService.searchPosterAsync(
                        movieCode,
                        title,
                        releaseDate
                );

            } catch (Exception e) {

                log.error(
                        "포스터 검색 작업 등록 실패. movieCode: {}",
                        movieCode,
                        e
                );
            }
        }
    }

    // =========================================================
    // 3. 영화 상세정보 조회
    // =========================================================

    @Transactional
    public MovieDetailResponse getMovieDetail(String movieCode) {

        // DB에 저장된 영화 정보가 있는지 먼저 확인
        Movie savedMovie = movieRepository.findById(movieCode)
                .orElse(null);

        if (savedMovie != null) {

            // 기존 영화의 누락 정보를 KMDb → TMDb 순서로 보완
            supplementMissingDetails(savedMovie);

            // 보완된 정보와 마지막 시도 시각을 DB에 저장
            movieRepository.save(savedMovie);

            // 최종 영화 정보를 응답 DTO로 변환
            return toDetailResponse(savedMovie);
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

            // KOBIS에 없는 상세정보를 KMDb → TMDb 순서로 보완
            supplementMissingDetails(newMovie);

            movieRepository.save(newMovie);

            return toDetailResponse(newMovie);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "영화 상세정보를 처리하지 못했습니다.",
                    e
            );
        }
    }

    // =========================================================
    // 4. 영화 상세정보 누락 항목 보완
    // KMDb → TMDb 순서
    // =========================================================

    private void supplementMissingDetails(Movie movie) {

        LocalDateTime now = LocalDateTime.now();

        // 누락 정보가 없거나 재시도 시간이 지나지 않았다면 보완 생략
        if (!hasMissingDetails(movie)
                || !movie.canAttemptSupplement(now)) {
            return;
        }

        // 보완 성공 여부와 관계없이 마지막 시도 시각 기록
        movie.markSupplementAttempted(now);

        // 1차: KMDb에서 동일 영화의 누락 정보 조회
        kmdbService.findMovieSupplement(
                        movie.getTitle(),
                        movie.getTitleEn(),
                        movie.getReleaseDate(),
                        movie.getDirector()
                )
                .ifPresent(candidate ->
                        applySupplement(movie, candidate)
                );

        // 2차: KMDb 보완 후에도 누락 정보가 있다면 TMDb 조회
        if (hasMissingDetails(movie)) {

            tmdbService.findMovieSupplement(
                            movie.getTitle(),
                            movie.getTitleEn(),
                            movie.getReleaseDate(),
                            movie.getDirector()
                    )
                    .ifPresent(candidate ->
                            applySupplement(movie, candidate)
                    );
        }
    }

    // =========================================================
    // 5. 외부 API 보완 정보를 Movie 엔티티에 적용
    // =========================================================

    private void applySupplement(
            Movie movie,
            MovieSupplement candidate) {

        movie.supplementMissingDetails(
                candidate.titleEn(),
                candidate.releaseDate(),
                candidate.showTime(),
                candidate.genre(),
                candidate.director(),
                String.join(",", candidate.actors()),
                candidate.watchGrade(),
                candidate.source()
        );
    }

    // =========================================================
    // 6. 영화 상세정보 누락 여부 확인
    // =========================================================

    private boolean hasMissingDetails(Movie movie) {

        return isBlank(movie.getTitleEn())
                || isBlank(movie.getReleaseDate())
                || isBlank(movie.getShowTime())
                || isBlank(movie.getGenre())
                || isBlank(movie.getDirector())
                || isBlank(movie.getActors())
                || isBlank(movie.getWatchGrade());
    }

    // null 또는 빈 문자열인지 확인
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // =========================================================
    // 7. Movie 엔티티를 응답 DTO로 변환
    // =========================================================

    private MovieDetailResponse toDetailResponse(Movie movie) {

        List<String> actors = isBlank(movie.getActors())
                ? List.of()
                : Arrays.asList(movie.getActors().split(","));

        return new MovieDetailResponse(
                movie.getMovieCode(),
                movie.getTitle(),
                movie.getTitleEn(),
                movie.getReleaseDate(),
                movie.getShowTime(),
                movie.getGenre(),
                movie.getDirector(),
                actors,
                movie.getWatchGrade()
        );
    }

    // =========================================================
    // 8. 일별 박스오피스 조회
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

                // KMDb → TMDb 순서로 포스터 검색
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
    // 9. 장르별 영화 목록 조회
    // =========================================================

    public List<MovieSearchResponse> getMoviesByGenre(
            String genre,
            int page) {

        List<MovieSearchResponse> movies = new ArrayList<>();

        // 페이지 번호와 장르 검증
        if (page < 1 || genre == null || genre.isBlank()) {
            throw new IllegalArgumentException(
                    "장르와 페이지 번호를 확인해주세요."
            );
        }

        String kobisGenre = switch (genre.trim()) {
            case "로맨스" -> "멜로/로맨스";
            case "공포" -> "공포(호러)";
            default -> genre.trim();
        };

        // 페이지당 10개 영화
        int skipCount = (page - 1) * 10;
        int matchCount = 0;

        // KOBIS 원본 영화 목록을 최대 10페이지까지 탐색
        for (int currentPage = 1; currentPage <= 10; currentPage++) {

            String json = kobisApiClient.searchMoviesByGenre(currentPage);

            try {

                JsonNode movieList = objectMapper.readTree(json)
                        .path("movieListResult")
                        .path("movieList");

                if (!movieList.isArray() || movieList.isEmpty()) {
                    break;
                }

                for (JsonNode movie : movieList) {

                    String movieGenre = movie.path("genreAlt").asText("");

                    boolean matchesGenre = Arrays.stream(
                                    movieGenre.split(",")
                            )
                            .map(String::trim)
                            .anyMatch(name -> name.equals(kobisGenre));

                    if (!matchesGenre) {
                        continue;
                    }

                    // 이전 페이지에 해당하는 영화는 건너뛰기
                    if (matchCount < skipCount) {
                        matchCount++;
                        continue;
                    }

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
                            movieGenre,
                            director
                    ));

                    // 요청한 10개를 모두 찾으면 종료
                    if (movies.size() >= 10) {

                        // 장르별 영화도 비동기 포스터 검색 등록
                        enqueuePosterSearches(movies);

                        return movies;
                    }
                }

            } catch (Exception e) {

                throw new IllegalStateException(
                        "장르별 영화 목록을 처리하지 못했습니다.",
                        e
                );
            }
        }

        // 10개 미만이어도 조회된 영화의 포스터 등록
        enqueuePosterSearches(movies);

        return movies;
    }

    // =========================================================
    // 10. 영화 코드로 포스터 조회 및 비동기 캐시 관리
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

        // 5. PENDING 상태인 경우에만 검색 작업 시작
        boolean started = posterCacheService
                .tryMarkAsProcessing(movieCode);

        if (started) {

            // 6. 백그라운드 포스터 검색 실행
            posterAsyncService.searchPosterAsync(
                    movieCode,
                    movie.title(),
                    movie.releaseDate()
            );
        }

        // 7. 포스터 검색 완료를 기다리지 않고 즉시 반환
        return new MoviePosterResponse(movieCode, "");
    }
}
