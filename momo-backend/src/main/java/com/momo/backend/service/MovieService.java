package com.momo.backend.service;

import com.momo.backend.client.KobisApiClient;
import com.momo.backend.dto.movie.BoxOfficeResponse;
import com.momo.backend.dto.movie.MovieDetailResponse;
import com.momo.backend.dto.movie.MoviePosterResponse;
import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.dto.movie.MovieSearchPageResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MovieCache;
import com.momo.backend.entity.MoviePosterCache;
import com.momo.backend.entity.MovieSupplement;
import com.momo.backend.entity.PosterStatus;
import com.momo.backend.repository.MovieCacheRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final MovieCacheRepository movieCacheRepository;
    private final MoviePosterCacheService posterCacheService;

    private static final String CACHE_SEARCH = "SEARCH";
    private static final String CACHE_DAILY_BOX_OFFICE = "DAILY_BOX_OFFICE";
    private static final String CACHE_GENRE = "GENRE";

    private static final long SEARCH_CACHE_HOURS = 24;
    private static final long DAILY_BOX_OFFICE_CACHE_HOURS = 1;
    private static final long GENRE_CACHE_HOURS = 24;

    private static final int GENRE_SCAN_PAGES = 10;
    private static final int GENRE_SCAN_SIZE = 100;
    private static final int GENRE_PAGE_SIZE = 10;

    // =========================================================
    // 통합 캐시 조회
    // =========================================================

    private MovieCache findValidCache(String cacheType, String cacheKey) {

        MovieCache cache = movieCacheRepository
                .findByCacheTypeAndCacheKey(cacheType, cacheKey)
                .orElse(null);

        if (cache == null || !cache.isValid(LocalDateTime.now())) {
            return null;
        }

        return cache;
    }

    // =========================================================
    // 통합 캐시 저장
    // =========================================================

    private void saveCache(
            String cacheType,
            String cacheKey,
            Object data,
            long ttlHours
    ) {

        try {

            String contentJson = objectMapper.writeValueAsString(data);

            MovieCache cache = movieCacheRepository
                    .findByCacheTypeAndCacheKey(cacheType, cacheKey)
                    .orElse(null);

            if (cache == null) {

                cache = new MovieCache(
                        cacheType,
                        cacheKey,
                        contentJson,
                        ttlHours
                );

            } else {

                cache.update(contentJson, ttlHours);
            }

            movieCacheRepository.save(cache);

        } catch (Exception e) {

            log.warn(
                    "영화 통합 캐시 저장 실패. type: {}, key: {}",
                    cacheType,
                    cacheKey,
                    e
            );
        }
    }

    // =========================================================
    // 공통 JSON 검증
    // =========================================================

    private JsonNode readRequiredArray(
            String json,
            String parentField,
            String arrayField,
            String errorMessage
    ) {

        try {

            JsonNode root = objectMapper.readTree(json);
            JsonNode parent = root.path(parentField);

            if (parent.isMissingNode() || parent.isNull()) {
                throw new IllegalStateException(errorMessage);
            }

            JsonNode result = parent.path(arrayField);

            if (!result.isArray()) {
                throw new IllegalStateException(errorMessage);
            }

            return result;

        } catch (Exception e) {

            throw new IllegalStateException(errorMessage, e);
        }
    }

    // =========================================================
    // 영화 검색 캐시 변환
    // =========================================================

    private MovieSearchPageResponse readCachedSearchPage(
            String contentJson
    ) throws Exception {

        JsonNode root = objectMapper.readTree(contentJson);

        List<MovieSearchResponse> movies = new ArrayList<>();

        for (JsonNode movie : root.path("content")) {

            movies.add(new MovieSearchResponse(
                    movie.path("movieCode").asText(""),
                    movie.path("title").asText(""),
                    movie.path("releaseDate").asText(""),
                    movie.path("genre").asText(""),
                    movie.path("director").asText("")
            ));
        }

        return new MovieSearchPageResponse(
                movies,
                root.path("page").asInt(),
                root.path("size").asInt(),
                root.path("totalCount").asInt(),
                root.path("hasNext").asBoolean()
        );
    }

    // =========================================================
    // 검색 결과를 Movie DB에 저장
    // =========================================================

    @Transactional
    private void saveSearchMoviesToDatabase(
            List<MovieSearchResponse> movies
    ) {

        if (movies == null || movies.isEmpty()) {
            return;
        }

        List<String> movieCodes = movies.stream()
                .map(MovieSearchResponse::movieCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();

        if (movieCodes.isEmpty()) {
            return;
        }

        Map<String, Movie> savedMovies = new HashMap<>();

        movieRepository.findAllById(movieCodes)
                .forEach(movie ->
                        savedMovies.put(movie.getMovieCode(), movie)
                );

        List<Movie> newMovies = new ArrayList<>();

        for (MovieSearchResponse item : movies) {

            String movieCode = item.movieCode();

            if (movieCode == null || movieCode.isBlank()
                    || item.title() == null || item.title().isBlank()) {
                continue;
            }

            Movie savedMovie = savedMovies.get(movieCode);

            if (savedMovie == null) {

                newMovies.add(new Movie(
                        movieCode,
                        item.title(),
                        "",
                        item.releaseDate(),
                        "",
                        item.genre(),
                        item.director(),
                        "",
                        ""
                ));

                continue;
            }

            // 기존 상세정보는 유지하고 비어 있는 항목만 채운다.
            savedMovie.supplementMissingDetails(
                    "",
                    item.releaseDate(),
                    "",
                    item.genre(),
                    item.director(),
                    "",
                    "",
                    "KOBIS_SEARCH"
            );
        }

        if (!newMovies.isEmpty()) {
            movieRepository.saveAll(newMovies);
        }

        log.info(
                "검색 영화 DB 저장 완료. 신규 저장: {}, 기존 영화: {}",
                newMovies.size(),
                savedMovies.size()
        );
    }

    // =========================================================
    // 영화 검색
    // =========================================================

    public MovieSearchPageResponse searchMovies(
            String keyword,
            int page,
            int size
    ) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        if (page < 1 || size < 1) {
            throw new IllegalArgumentException(
                    "페이지와 검색 개수는 1 이상이어야 합니다."
            );
        }

        String searchKeyword = keyword.trim()
                .replaceAll("\\s+", " ");

        String cacheKey = searchKeyword + "|" + page + "|" + size;

        // 1. 검색 캐시 우선 조회
        MovieCache cached = findValidCache(
                CACHE_SEARCH,
                cacheKey
        );

        if (cached != null) {

            try {

                MovieSearchPageResponse response =
                        readCachedSearchPage(cached.getContentJson());

                // 캐시 결과도 DB에 누락되어 있다면 저장
                saveSearchMoviesToDatabase(response.content());

                enqueuePosterSearches(response.content());

                return response;

            } catch (Exception e) {

                log.warn("검색 캐시 변환 실패. KOBIS 재조회 진행", e);
            }
        }

        // 2. 캐시가 없으면 KOBIS 검색
        String json = kobisApiClient.searchMovies(
                searchKeyword,
                page,
                size
        );

        JsonNode movieList = readRequiredArray(
                json,
                "movieListResult",
                "movieList",
                "KOBIS 영화 검색 응답이 올바르지 않습니다."
        );

        JsonNode root;

        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("KOBIS 검색 응답 변환 실패", e);
        }

        int totalCount = root.path("movieListResult")
                .path("totCnt")
                .asInt(0);

        List<MovieSearchResponse> movies = new ArrayList<>();

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

        // 3. 검색 결과를 DB에 저장
        saveSearchMoviesToDatabase(movies);

        // 4. 포스터 검색은 캐시 상태를 확인한 후 비동기 실행
        enqueuePosterSearches(movies);

        boolean hasNext = (long) page * size < totalCount;

        MovieSearchPageResponse response =
                new MovieSearchPageResponse(
                        movies,
                        page,
                        size,
                        totalCount,
                        hasNext
                );

        // 5. 검색 결과 캐시 저장
        saveCache(
                CACHE_SEARCH,
                cacheKey,
                response,
                SEARCH_CACHE_HOURS
        );

        return response;
    }

    // =========================================================
    // 포스터 비동기 검색 등록
    // =========================================================

    private void enqueuePosterSearches(
            List<MovieSearchResponse> movies
    ) {

        if (movies == null || movies.isEmpty()) {
            return;
        }

        for (MovieSearchResponse movie : movies) {

            String movieCode = movie.movieCode();
            String title = movie.title();
            String releaseDate = movie.releaseDate();

            if (isBlank(movieCode) || isBlank(title)) {
                continue;
            }

            try {

                // DB에 포스터가 있으면 외부 API를 호출하지 않는다.
                Movie savedMovie = movieRepository.findById(movieCode)
                        .orElse(null);

                if (savedMovie != null
                        && !isBlank(savedMovie.getPosterUrl())) {
                    continue;
                }

                posterCacheService.createCache(
                        movieCode,
                        title,
                        releaseDate
                );

                boolean started =
                        posterCacheService.tryMarkAsProcessing(movieCode);

                if (!started) {
                    continue;
                }

                posterAsyncService.searchPosterAsync(
                        movieCode,
                        title,
                        releaseDate
                );

            } catch (Exception e) {

                log.error(
                        "포스터 검색 등록 실패. movieCode: {}",
                        movieCode,
                        e
                );
            }
        }
    }

    // =========================================================
    // 영화 상세정보 조회
    // =========================================================

    @Transactional
    public MovieDetailResponse getMovieDetail(String movieCode) {

        if (isBlank(movieCode)) {
            throw new IllegalArgumentException("영화 코드가 필요합니다.");
        }

        Movie savedMovie = movieRepository.findById(movieCode)
                .orElse(null);

        // 1. DB에 저장된 영화가 있는 경우
        if (savedMovie != null) {

            // 상세정보가 이미 충분하면 외부 API 호출 없음
            if (!hasMissingDetails(savedMovie)) {
                return toDetailResponse(savedMovie);
            }

            // 검색 목록에서 생성된 부분 정보라면 KOBIS 상세정보로 보완
            supplementFromKobis(savedMovie);

            // KOBIS에서 누락된 정보만 KMDb/TMDb로 보완
            supplementMissingDetails(savedMovie);

            movieRepository.save(savedMovie);

            return toDetailResponse(savedMovie);
        }

        // 2. DB에 영화가 없는 경우에만 KOBIS 호출
        Movie newMovie = fetchMovieDetailFromKobis(movieCode);

        supplementMissingDetails(newMovie);

        movieRepository.save(newMovie);

        return toDetailResponse(newMovie);
    }

    // =========================================================
    // KOBIS 상세정보 조회 및 DB 항목 보완
    // =========================================================

    private void supplementFromKobis(Movie movie) {

        String json = kobisApiClient.getMovieDetail(
                movie.getMovieCode()
        );

        try {

            JsonNode detail = objectMapper.readTree(json)
                    .path("movieInfoResult")
                    .path("movieInfo");

            if (detail.isMissingNode() || detail.isEmpty()) {
                throw new IllegalStateException(
                        "KOBIS 상세정보가 존재하지 않습니다."
                );
            }

            String director = "";

            JsonNode directors = detail.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {
                director = directors.get(0)
                        .path("peopleNm")
                        .asText("");
            }

            String genre = StreamSupport.stream(
                            detail.path("genres").spliterator(),
                            false
                    )
                    .map(node -> node.path("genreNm").asText(""))
                    .filter(value -> !value.isBlank())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            List<String> actors = new ArrayList<>();

            for (JsonNode actor : detail.path("actors")) {

                String name = actor.path("peopleNm").asText("");

                if (!name.isBlank()) {
                    actors.add(name);
                }

                if (actors.size() >= 10) {
                    break;
                }
            }

            String watchGrade = "";

            JsonNode audits = detail.path("audits");

            if (audits.isArray() && !audits.isEmpty()) {
                watchGrade = audits.get(0)
                        .path("watchGradeNm")
                        .asText("");
            }

            movie.supplementMissingDetails(
                    detail.path("movieNmEn").asText(""),
                    detail.path("openDt").asText(""),
                    detail.path("showTm").asText(""),
                    genre,
                    director,
                    String.join(",", actors),
                    watchGrade,
                    "KOBIS"
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "KOBIS 상세정보 보완에 실패했습니다.",
                    e
            );
        }
    }

    private Movie fetchMovieDetailFromKobis(String movieCode) {

        String json = kobisApiClient.getMovieDetail(movieCode);

        try {

            JsonNode detail = objectMapper.readTree(json)
                    .path("movieInfoResult")
                    .path("movieInfo");

            if (detail.isMissingNode() || detail.isEmpty()) {
                throw new IllegalStateException(
                        "KOBIS 영화 상세정보가 없습니다."
                );
            }

            String director = "";

            JsonNode directors = detail.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {
                director = directors.get(0)
                        .path("peopleNm")
                        .asText("");
            }

            String genre = StreamSupport.stream(
                            detail.path("genres").spliterator(),
                            false
                    )
                    .map(node -> node.path("genreNm").asText(""))
                    .filter(value -> !value.isBlank())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            List<String> actors = new ArrayList<>();

            for (JsonNode actor : detail.path("actors")) {

                String name = actor.path("peopleNm").asText("");

                if (!name.isBlank()) {
                    actors.add(name);
                }

                if (actors.size() >= 10) {
                    break;
                }
            }

            String watchGrade = "";

            JsonNode audits = detail.path("audits");

            if (audits.isArray() && !audits.isEmpty()) {
                watchGrade = audits.get(0)
                        .path("watchGradeNm")
                        .asText("");
            }

            return new Movie(
                    detail.path("movieCd").asText(movieCode),
                    detail.path("movieNm").asText(""),
                    detail.path("movieNmEn").asText(""),
                    detail.path("openDt").asText(""),
                    detail.path("showTm").asText(""),
                    genre,
                    director,
                    String.join(",", actors),
                    watchGrade
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "영화 상세정보 처리 실패",
                    e
            );
        }
    }

    // =========================================================
    // 외부 API 상세정보 보완
    // =========================================================

    private void supplementMissingDetails(Movie movie) {

        LocalDateTime now = LocalDateTime.now();

        if (!hasMissingDetails(movie)
                || !movie.canAttemptSupplement(now)) {
            return;
        }

        movie.markSupplementAttempted(now);

        kmdbService.findMovieSupplement(
                        movie.getTitle(),
                        movie.getTitleEn(),
                        movie.getReleaseDate(),
                        movie.getDirector()
                )
                .ifPresent(candidate ->
                        applySupplement(movie, candidate)
                );

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

    private void applySupplement(
            Movie movie,
            MovieSupplement candidate
    ) {

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

    private boolean hasMissingDetails(Movie movie) {

        return isBlank(movie.getTitleEn())
                || isBlank(movie.getReleaseDate())
                || isBlank(movie.getShowTime())
                || isBlank(movie.getGenre())
                || isBlank(movie.getDirector())
                || isBlank(movie.getActors())
                || isBlank(movie.getWatchGrade());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

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
    // 박스오피스 캐시 변환
    // =========================================================

    private List<BoxOfficeResponse> readCachedBoxOffice(
            String contentJson
    ) throws Exception {

        List<BoxOfficeResponse> movies = new ArrayList<>();

        JsonNode root = objectMapper.readTree(contentJson);

        for (JsonNode movie : root) {

            movies.add(new BoxOfficeResponse(
                    movie.path("rank").asInt(),
                    movie.path("movieCode").asText(""),
                    movie.path("title").asText(""),
                    movie.path("releaseDate").asText(""),
                    movie.path("audienceCount").asText(""),
                    movie.path("totalAudience").asText(""),
                    movie.path("posterUrl").asText("")
            ));
        }

        return movies;
    }

    // =========================================================
    // DB와 포스터 캐시를 이용한 포스터 조회
    // =========================================================

    private String findSavedPosterUrl(String movieCode) {

        if (isBlank(movieCode)) {
            return "";
        }

        // 1. Movie 테이블의 포스터
        Movie movie = movieRepository.findById(movieCode)
                .orElse(null);

        if (movie != null && !isBlank(movie.getPosterUrl())) {
            return movie.getPosterUrl();
        }

        // 2. MoviePosterCache의 포스터
        MoviePosterCache posterCache = posterCacheService
                .findCache(movieCode)
                .orElse(null);

        if (posterCache != null
                && posterCache.getStatus() == PosterStatus.SUCCESS
                && !isBlank(posterCache.getPosterUrl())) {

            return posterCache.getPosterUrl();
        }

        return "";
    }

    // =========================================================
    // 포스터가 없는 영화만 비동기 검색
    // =========================================================

    private void enqueuePosterSearch(
            String movieCode,
            String title,
            String releaseDate
    ) {

        if (isBlank(movieCode) || isBlank(title)) {
            return;
        }

        try {

            if (!findSavedPosterUrl(movieCode).isBlank()) {
                return;
            }

            posterCacheService.createCache(
                    movieCode,
                    title,
                    releaseDate
            );

            boolean started =
                    posterCacheService.tryMarkAsProcessing(movieCode);

            if (!started) {
                return;
            }

            posterAsyncService.searchPosterAsync(
                    movieCode,
                    title,
                    releaseDate
            );

        } catch (Exception e) {

            log.error(
                    "포스터 검색 등록 실패. movieCode: {}",
                    movieCode,
                    e
            );
        }
    }

    // =========================================================
    // 박스오피스 포스터 캐시 보완
    // =========================================================

    private List<BoxOfficeResponse> enrichBoxOfficePosters(
            List<BoxOfficeResponse> movies
    ) {

        List<BoxOfficeResponse> result = new ArrayList<>();

        for (BoxOfficeResponse item : movies) {

            String posterUrl = findSavedPosterUrl(item.movieCode());

            if (posterUrl.isBlank()) {

                enqueuePosterSearch(
                        item.movieCode(),
                        item.title(),
                        item.releaseDate()
                );

                posterUrl = item.posterUrl() == null
                        ? ""
                        : item.posterUrl();

            }

            result.add(new BoxOfficeResponse(
                    item.rank(),
                    item.movieCode(),
                    item.title(),
                    item.releaseDate(),
                    item.audienceCount(),
                    item.totalAudience(),
                    posterUrl
            ));
        }

        return result;
    }

    // =========================================================
    // 일별 박스오피스 조회
    // =========================================================

    public List<BoxOfficeResponse> getDailyBoxOffice(
            String targetDate
    ) {

        if (targetDate == null
                || !targetDate.matches("\\d{8}")) {
            throw new IllegalArgumentException(
                    "박스오피스 날짜는 YYYYMMDD 형식이어야 합니다."
            );
        }

        // 1. 날짜별 캐시 조회
        MovieCache cached = findValidCache(
                CACHE_DAILY_BOX_OFFICE,
                targetDate
        );

        if (cached != null) {

            try {

                return enrichBoxOfficePosters(
                        readCachedBoxOffice(cached.getContentJson())
                );

            } catch (Exception e) {

                log.warn("박스오피스 캐시 변환 실패. KOBIS 재조회", e);
            }
        }

        // 2. 캐시가 없으면 KOBIS 호출
        String json = kobisApiClient.getDailyBoxOffice(targetDate);

        JsonNode boxOfficeList = readRequiredArray(
                json,
                "boxOfficeResult",
                "dailyBoxOfficeList",
                "KOBIS 박스오피스 응답이 올바르지 않습니다."
        );

        List<BoxOfficeResponse> movies = new ArrayList<>();

        List<MovieSearchResponse> searchResults = new ArrayList<>();

        for (JsonNode item : boxOfficeList) {

            String movieCode = item.path("movieCd").asText("");
            String title = item.path("movieNm").asText("");
            String releaseDate = item.path("openDt").asText("");

            searchResults.add(new MovieSearchResponse(
                    movieCode,
                    title,
                    releaseDate,
                    "",
                    ""
            ));

            movies.add(new BoxOfficeResponse(
                    item.path("rank").asInt(),
                    movieCode,
                    title,
                    releaseDate,
                    item.path("audiCnt").asText(""),
                    item.path("audiAcc").asText(""),
                    findSavedPosterUrl(movieCode)
            ));
        }

        // 3. 박스오피스 영화도 Movie DB에 저장
        saveSearchMoviesToDatabase(searchResults);

        // 4. 포스터 캐시를 확인하고 없는 영화만 비동기 검색
        List<BoxOfficeResponse> enriched =
                enrichBoxOfficePosters(movies);

        // 5. 날짜별 박스오피스 결과 캐시 저장
        saveCache(
                CACHE_DAILY_BOX_OFFICE,
                targetDate,
                enriched,
                DAILY_BOX_OFFICE_CACHE_HOURS
        );

        return enriched;
    }

    // =========================================================
    // 장르 이름 정규화
    // =========================================================

    private String normalizeGenre(String genre) {

        return switch (genre.trim()) {
            case "로맨스" -> "멜로/로맨스";
            case "공포" -> "공포(호러)";
            default -> genre.trim();
        };
    }

    // =========================================================
    // 장르 캐시 JSON 변환
    // =========================================================

    private List<MovieSearchResponse> readCachedGenreMovies(
            String contentJson
    ) throws Exception {

        List<MovieSearchResponse> movies = new ArrayList<>();

        JsonNode root = objectMapper.readTree(contentJson);

        for (JsonNode movie : root) {

            movies.add(new MovieSearchResponse(
                    movie.path("movieCode").asText(""),
                    movie.path("title").asText(""),
                    movie.path("releaseDate").asText(""),
                    movie.path("genre").asText(""),
                    movie.path("director").asText("")
            ));
        }

        return movies;
    }

    // =========================================================
    // 장르 전체 목록 수집
    // =========================================================

    private List<MovieSearchResponse> fetchAllMoviesByGenre(
            String genre
    ) {

        List<MovieSearchResponse> result = new ArrayList<>();

        for (int currentPage = 1;
             currentPage <= GENRE_SCAN_PAGES;
             currentPage++) {

            String json = kobisApiClient.searchMoviesByGenre(currentPage);

            JsonNode movieList = readRequiredArray(
                    json,
                    "movieListResult",
                    "movieList",
                    "KOBIS 장르 검색용 영화 목록 응답이 올바르지 않습니다."
            );

            if (movieList.isEmpty()) {
                break;
            }

            for (JsonNode movie : movieList) {

                String movieGenre = movie.path("genreAlt").asText("");

                boolean matchesGenre = Arrays.stream(
                                movieGenre.split(",")
                        )
                        .map(String::trim)
                        .anyMatch(name -> name.equals(genre));

                if (!matchesGenre) {
                    continue;
                }

                String director = "";

                JsonNode directors = movie.path("directors");

                if (directors.isArray() && !directors.isEmpty()) {
                    director = directors.get(0)
                            .path("peopleNm")
                            .asText("");
                }

                result.add(new MovieSearchResponse(
                        movie.path("movieCd").asText(""),
                        movie.path("movieNm").asText(""),
                        movie.path("openDt").asText(""),
                        movieGenre,
                        director
                ));
            }

            if (movieList.size() < GENRE_SCAN_SIZE) {
                break;
            }
        }

        return result;
    }

    // =========================================================
    // 장르별 영화 조회
    // =========================================================

    public List<MovieSearchResponse> getMoviesByGenre(
            String genre,
            int page
    ) {

        if (isBlank(genre) || page < 1) {
            throw new IllegalArgumentException(
                    "장르와 페이지 번호를 확인해주세요."
            );
        }

        String normalizedGenre = normalizeGenre(genre);

        // 페이지가 아닌 장르 하나를 캐시 식별값으로 사용
        String cacheKey = normalizedGenre;

        List<MovieSearchResponse> allMovies;

        MovieCache cached = findValidCache(
                CACHE_GENRE,
                cacheKey
        );

        if (cached != null) {

            try {

                allMovies = readCachedGenreMovies(
                        cached.getContentJson()
                );

            } catch (Exception e) {

                log.warn("장르 캐시 변환 실패. KOBIS 재조회", e);

                allMovies = fetchAllMoviesByGenre(normalizedGenre);

                saveCache(
                        CACHE_GENRE,
                        cacheKey,
                        allMovies,
                        GENRE_CACHE_HOURS
                );
            }

        } else {

            // 캐시가 없을 때만 KOBIS 목록 수집
            allMovies = fetchAllMoviesByGenre(normalizedGenre);

            // 전체 결과를 한 번에 캐싱
            saveCache(
                    CACHE_GENRE,
                    cacheKey,
                    allMovies,
                    GENRE_CACHE_HOURS
            );
        }

        // 전체 결과를 DB에 저장
        saveSearchMoviesToDatabase(allMovies);

        // 페이지는 캐시된 전체 목록에서 분리
        int start = (page - 1) * GENRE_PAGE_SIZE;

        if (start >= allMovies.size()) {
            return List.of();
        }

        int end = Math.min(
                start + GENRE_PAGE_SIZE,
                allMovies.size()
        );

        List<MovieSearchResponse> pageMovies =
                allMovies.subList(start, end);

        enqueuePosterSearches(pageMovies);

        return pageMovies;
    }

    // =========================================================
    // 포스터 조회 API
    // =========================================================

    public MoviePosterResponse getMoviePoster(String movieCode) {

        if (isBlank(movieCode)) {
            throw new IllegalArgumentException("영화 코드가 필요합니다.");
        }

        // 1. Movie DB 포스터 우선 조회
        String savedPoster = findSavedPosterUrl(movieCode);

        if (!savedPoster.isBlank()) {
            return new MoviePosterResponse(movieCode, savedPoster);
        }

        // 2. DB에 포스터가 없으면 영화 정보 조회
        Movie movie = movieRepository.findById(movieCode)
                .orElse(null);

        if (movie == null) {

            MovieDetailResponse detail = getMovieDetail(movieCode);

            enqueuePosterSearch(
                    movieCode,
                    detail.title(),
                    detail.releaseDate()
            );

            return new MoviePosterResponse(movieCode, "");
        }

        // 3. 포스터 캐시를 확인하고 없을 때만 검색
        enqueuePosterSearch(
                movieCode,
                movie.getTitle(),
                movie.getReleaseDate()
        );

        return new MoviePosterResponse(movieCode, "");
    }
}