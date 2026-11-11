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
import com.momo.backend.repository.MoviePosterCacheRepository;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final MoviePersistenceService moviePersistenceService;
    private final MoviePosterCacheRepository moviePosterCacheRepository;
    private final PosterFileStorageService posterFileStorageService;
    private final Cache<String, MovieCache> movieApiMemoryCache;

    private static final Object[] CACHE_LOCKS = createCacheLocks();

    private static Object[] createCacheLocks() {
        Object[] locks = new Object[256];
        Arrays.setAll(locks, index -> new Object());
        return locks;
    }

    private static final String CACHE_SEARCH = "SEARCH";
    private static final String CACHE_DAILY_BOX_OFFICE = "DAILY_BOX_OFFICE";

    private static final long SEARCH_CACHE_HOURS = 24;
    private static final long DAILY_BOX_OFFICE_CACHE_HOURS = 1;

    // KOBIS 목록은 페이지당 최대 100개씩 전체 페이지를 순회한다.
    private static final int GENRE_SCAN_SIZE = 100;
    private static final int GENRE_PAGE_SIZE = 10;

    // 통합 캐시 조회

    private MovieCache findValidCache(String cacheType, String cacheKey) {

        String memoryKey = cacheType + "::" + cacheKey;
        LocalDateTime now = LocalDateTime.now();

        // L1: 백엔드 메모리 캐시를 먼저 확인한다.
        MovieCache memoryCache = movieApiMemoryCache.getIfPresent(memoryKey);
        if (memoryCache != null) {
            if (memoryCache.isValid(now)) {
                log.debug("영화 메모리 캐시 HIT. type: {}, key: {}", cacheType, cacheKey);
                return memoryCache;
            }
            movieApiMemoryCache.invalidate(memoryKey);
        }

        // L2: 메모리에 없으면 기존 MySQL 캐시를 확인한다.
        MovieCache databaseCache = movieCacheRepository
                .findByCacheTypeAndCacheKey(cacheType, cacheKey)
                .orElse(null);

        if (databaseCache == null || !databaseCache.isValid(now)) {
            log.info("영화 API 캐시 MISS. type: {}, key: {}", cacheType, cacheKey);
            return null;
        }

        // MySQL에서 찾은 유효한 데이터를 메모리에도 올린다.
        movieApiMemoryCache.put(memoryKey, databaseCache);
        log.debug("영화 MySQL 캐시 HIT. 메모리 캐시에 적재. type: {}, key: {}", cacheType, cacheKey);
        return databaseCache;
    }

    // 통합 캐시 저장

    private void saveCache(
            String cacheType,
            String cacheKey,
            Object data,
            long ttlHours) {

        try {

            String contentJson = objectMapper.writeValueAsString(data);

            LocalDateTime now = LocalDateTime.now();
            movieCacheRepository.upsert(cacheType, cacheKey, contentJson,
                    now, now.plusHours(ttlHours));

            // DB 저장이 성공한 데이터만 L1 메모리 캐시에 반영한다.
            String memoryKey = cacheType + "::" + cacheKey;
            movieApiMemoryCache.put(
                    memoryKey,
                    new MovieCache(cacheType, cacheKey, contentJson, ttlHours));

        } catch (Exception e) {

            log.warn(
                    "영화 통합 캐시 저장 실패. type: {}, key: {}",
                    cacheType,
                    cacheKey,
                    e);
        }
    }

    // 공통 JSON 검증

    private JsonNode readRequiredArray(
            String json,
            String parentField,
            String arrayField,
            String errorMessage) {

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

    private JsonNode readRequiredMovieDetail(String json, String requestedMovieCode) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode detail = root.path("movieInfoResult").path("movieInfo");
            String responseCode = detail.path("movieCd").asString("");
            String title = detail.path("movieNm").asString("");
            if (!detail.isObject() || isBlank(responseCode) || isBlank(title)) {
                throw new IllegalStateException("KOBIS 상세 응답에 영화 코드 또는 제목이 없습니다.");
            }
            if (!requestedMovieCode.equals(responseCode)) {
                throw new IllegalStateException("KOBIS 상세 응답의 영화 코드가 요청한 영화 코드와 일치하지 않습니다.");
            }
            validateOptionalArray(detail, "directors");
            validateOptionalArray(detail, "genres");
            validateOptionalArray(detail, "actors");
            validateOptionalArray(detail, "audits");
            return detail;
        } catch (Exception e) {
            throw new IllegalStateException("KOBIS 상세 응답 구조가 올바르지 않습니다.", e);
        }
    }

    private void validateOptionalArray(JsonNode parent, String fieldName) {
        JsonNode value = parent.path(fieldName);
        if (!value.isMissingNode() && !value.isNull() && !value.isArray()) {
            throw new IllegalStateException("KOBIS 상세 응답 필드 " + fieldName + " 형식이 배열이 아닙니다.");
        }
    }

    // 영화 검색 캐시 변환

    private MovieSearchPageResponse readCachedSearchPage(
            String contentJson) throws Exception {

        JsonNode root = objectMapper.readTree(contentJson);
        if (!root.path("content").isArray()
                || !root.path("page").canConvertToInt()
                || !root.path("size").canConvertToInt()
                || !root.path("totalCount").canConvertToInt()
                || !root.path("hasNext").isBoolean()) {
            throw new IllegalStateException("검색 캐시 필수 필드가 올바르지 않습니다.");
        }

        List<MovieSearchResponse> movies = new ArrayList<>();

        for (JsonNode movie : root.path("content")) {
            if (isBlank(movie.path("movieCode").asString(""))
                    || isBlank(movie.path("title").asString(""))) {
                throw new IllegalStateException("검색 캐시에 영화 코드 또는 제목이 없습니다.");
            }
            movies.add(new MovieSearchResponse(
                    movie.path("movieCode").asString(""),
                    movie.path("title").asString(""),
                    movie.path("releaseDate").asString(""),
                    movie.path("genre").asString(""),
                    movie.path("director").asString("")));
        }

        return new MovieSearchPageResponse(
                movies,
                root.path("page").asInt(),
                root.path("size").asInt(),
                root.path("totalCount").asInt(),
                root.path("hasNext").asBoolean());
    }

    // 검색 결과를 Movie DB에 저장

    private void saveSearchMoviesToDatabase(List<MovieSearchResponse> movies) {
        moviePersistenceService.saveSearchMovies(movies);
    }

    // 영화 검색

    public MovieSearchPageResponse searchMovies(
            String keyword,
            int page,
            int size) {
        String lockKey = "SEARCH|" + (keyword == null ? "" : keyword.trim()) + "|" + page + "|" + size;
        Object lock = CACHE_LOCKS[Math.floorMod(lockKey.hashCode(), CACHE_LOCKS.length)];
        synchronized (lock) {
            return searchMoviesInternal(keyword, page, size);
        }
    }

    private MovieSearchPageResponse searchMoviesInternal(String keyword, int page, int size) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        if (page < 1 || size < 1) {
            throw new IllegalArgumentException(
                    "페이지와 검색 개수는 1 이상이어야 합니다.");
        }

        String searchKeyword = keyword.trim()
                .replaceAll("\\s+", " ");

        String cacheKey = searchKeyword + "|" + page + "|" + size;

        // 1. 검색 캐시 우선 조회
        MovieCache cached = findValidCache(
                CACHE_SEARCH,
                cacheKey);

        if (cached != null) {

            try {

                MovieSearchPageResponse response = readCachedSearchPage(cached.getContentJson());

                // 캐시 결과도 DB에 누락되어 있다면 저장
                saveSearchMoviesToDatabase(response.content());

                enqueuePosterSearches(response.content());

                return response;

            } catch (Exception e) {

                log.warn("검색 캐시 변환 실패. KOBIS 재조회 진행", e);
            }
        }

        // 2. 캐시가 없으면 KOBIS 검색
        log.info("KOBIS 영화 검색 API 요청. keyword: {}, page: {}, size: {}",
                searchKeyword, page, size);
        String json = kobisApiClient.searchMovies(searchKeyword, page, size);

        JsonNode movieList = readRequiredArray(
                json,
                "movieListResult",
                "movieList",
                "KOBIS 영화 검색 응답이 올바르지 않습니다.");

        JsonNode root;

        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("KOBIS 검색 응답 변환 실패", e);
        }

        JsonNode totalCountNode = root.path("movieListResult").path("totCnt");
        if (!totalCountNode.canConvertToInt() || totalCountNode.asInt() < 0) {
            throw new IllegalStateException("KOBIS 검색 총건수(totCnt)가 올바르지 않습니다.");
        }
        int totalCount = totalCountNode.asInt();

        List<MovieSearchResponse> movies = new ArrayList<>();

        for (JsonNode movie : movieList) {

            if (isBlank(movie.path("movieCd").asString(""))
                    || isBlank(movie.path("movieNm").asString(""))) {
                throw new IllegalStateException("KOBIS 검색 결과에 영화 코드 또는 제목이 누락되었습니다.");
            }
            String director = "";

            JsonNode directors = movie.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {
                director = directors.get(0)
                        .path("peopleNm")
                        .asString("");
            }

            movies.add(new MovieSearchResponse(
                    movie.path("movieCd").asString(""),
                    movie.path("movieNm").asString(""),
                    movie.path("openDt").asString(""),
                    movie.path("genreAlt").asString(""),
                    director));
        }

        // 3. 검색 결과를 DB에 저장
        saveSearchMoviesToDatabase(movies);

        // 4. 포스터 검색은 캐시 상태를 확인한 후 비동기 실행
        enqueuePosterSearches(movies);

        boolean hasNext = (long) page * size < totalCount;

        MovieSearchPageResponse response = new MovieSearchPageResponse(
                movies,
                page,
                size,
                totalCount,
                hasNext);

        // 5. 검색 결과 캐시 저장
        saveCache(
                CACHE_SEARCH,
                cacheKey,
                response,
                SEARCH_CACHE_HOURS);

        return response;
    }

    // 전체 카탈로그 수집 결과도 기존 포스터 중복 방지·비동기 작업 흐름을 재사용한다.
    public void enqueueCatalogPosterSearches(List<MovieSearchResponse> movies) {
        enqueuePosterSearches(movies, 900);
    }

    // 포스터 비동기 검색 등록

    private void enqueuePosterSearches(List<MovieSearchResponse> movies) {
        enqueuePosterSearches(movies, 200);
    }

    private void enqueuePosterSearches(
            List<MovieSearchResponse> movies,
            int priority) {

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
                        releaseDate,
                        priority);

                String claimToken = posterCacheService.tryMarkAsProcessing(movieCode);

                if (claimToken == null) {
                    continue;
                }

                try {
                    posterAsyncService.searchPosterAsync(
                            movieCode,
                            title,
                            releaseDate,
                            claimToken);
                } catch (RuntimeException schedulingException) {
                    // 스레드 풀 포화 등으로 작업 등록에 실패하면 PROCESSING 점유를 즉시 해제한다.
                    posterCacheService.releaseProcessingClaim(movieCode, claimToken);
                    throw schedulingException;
                }

            } catch (Exception e) {

                log.error(
                        "포스터 검색 등록 실패. movieCode: {}",
                        movieCode,
                        e);
            }
        }
    }

    // 영화 상세정보 조회

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
                supplementOverview(savedMovie);
                movieRepository.save(savedMovie);
                return toDetailResponse(savedMovie);
            }

            // 검색 목록에서 생성된 부분 정보라면 KOBIS 상세정보로 보완
            if (savedMovie.canAttemptKobisDetail(LocalDateTime.now())) {
                savedMovie.markKobisDetailAttempted(LocalDateTime.now());
                try {
                    log.info("KOBIS 상세정보 API 요청. movieCode: {}", movieCode);
                    supplementFromKobis(savedMovie);
                } catch (Exception e) {
                    log.warn("KOBIS 상세정보 보완 실패. KOBIS 검색 결과와 외부 API로 추가 보완합니다. movieCode: {}",
                            movieCode, e);
                }

                // 상세 API에 누락된 개봉일·감독은 KOBIS 영화 목록에서 같은 영화 코드로 재확인한다.
                supplementFromKobisSearch(savedMovie);
            }
            supplementMissingDetails(savedMovie);
            supplementOverview(savedMovie);

            movieRepository.save(savedMovie);

            return toDetailResponse(savedMovie);
        }

        // 2. DB에 영화가 없는 경우에만 KOBIS 호출
        log.info("KOBIS 상세정보 API 요청. movieCode: {}", movieCode);
        Movie newMovie = fetchMovieDetailFromKobis(movieCode);
        newMovie.markKobisDetailAttempted(LocalDateTime.now());
        supplementFromKobisSearch(newMovie);
        supplementMissingDetails(newMovie);
        supplementOverview(newMovie);

        movieRepository.save(newMovie);

        return toDetailResponse(newMovie);
    }

    // KOBIS 상세 API에 일부 항목이 없으면 영화 목록 API에서 같은 영화 코드를 다시 확인한다.
    private void supplementFromKobisSearch(Movie movie) {

        if (movie == null
                || (!isBlank(movie.getReleaseDate())
                        && !isBlank(movie.getDirector())
                        && !isBlank(movie.getGenre()))) {
            return;
        }

        List<String> searchTitles = new ArrayList<>();

        if (!isBlank(movie.getTitle())) {
            searchTitles.add(movie.getTitle());
        }

        if (!isBlank(movie.getTitleEn())
                && !searchTitles.contains(movie.getTitleEn())) {
            searchTitles.add(movie.getTitleEn());
        }

        for (String searchTitle : searchTitles) {
            try {
                String json = kobisApiClient.searchMovies(searchTitle, 1, 100);
                JsonNode movieList = readRequiredArray(
                        json,
                        "movieListResult",
                        "movieList",
                        "KOBIS 영화 목록 보완 응답이 올바르지 않습니다.");

                for (JsonNode candidate : movieList) {
                    if (!movie.getMovieCode().equals(
                            candidate.path("movieCd").asString(""))) {
                        continue;
                    }

                    String director = StreamSupport.stream(
                            candidate.path("directors").spliterator(),
                            false)
                            .map(node -> node.path("peopleNm").asString(""))
                            .filter(value -> !value.isBlank())
                            .distinct()
                            .limit(5)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");

                    movie.supplementMissingDetails(
                            candidate.path("movieNmEn").asString(""),
                            candidate.path("openDt").asString(""),
                            "",
                            candidate.path("genreAlt").asString(""),
                            director,
                            "",
                            "",
                            "KOBIS_SEARCH");

                    log.info("KOBIS 영화 목록으로 상세정보 보완 완료. movieCode: {}, searchTitle: {}",
                            movie.getMovieCode(), searchTitle);
                    return;
                }
            } catch (Exception e) {
                log.warn("KOBIS 영화 목록 보완 조회 실패. movieCode: {}, searchTitle: {}",
                        movie.getMovieCode(), searchTitle, e);
            }
        }
    }

    // KOBIS 상세정보 조회 및 DB 항목 보완

    private void supplementFromKobis(Movie movie) {

        String json = kobisApiClient.getMovieDetail(
                movie.getMovieCode());

        try {

            JsonNode detail = readRequiredMovieDetail(json, movie.getMovieCode());

            String director = "";

            JsonNode directors = detail.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {
                director = directors.get(0)
                        .path("peopleNm")
                        .asString("");
            }

            String genre = StreamSupport.stream(
                    detail.path("genres").spliterator(),
                    false)
                    .map(node -> node.path("genreNm").asString(""))
                    .filter(value -> !value.isBlank())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            List<String> actors = new ArrayList<>();

            for (JsonNode actor : detail.path("actors")) {

                String name = actor.path("peopleNm").asString("");

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
                        .asString("");
            }

            movie.supplementMissingDetails(
                    detail.path("movieNmEn").asString(""),
                    detail.path("openDt").asString(""),
                    detail.path("showTm").asString(""),
                    genre,
                    director,
                    String.join(",", actors),
                    watchGrade,
                    "KOBIS");

        } catch (Exception e) {

            throw new IllegalStateException(
                    "KOBIS 상세정보 보완에 실패했습니다.",
                    e);
        }
    }

    private Movie fetchMovieDetailFromKobis(String movieCode) {

        String json = kobisApiClient.getMovieDetail(movieCode);

        try {

            JsonNode detail = readRequiredMovieDetail(json, movieCode);

            String director = "";

            JsonNode directors = detail.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {
                director = directors.get(0)
                        .path("peopleNm")
                        .asString("");
            }

            String genre = StreamSupport.stream(
                    detail.path("genres").spliterator(),
                    false)
                    .map(node -> node.path("genreNm").asString(""))
                    .filter(value -> !value.isBlank())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            List<String> actors = new ArrayList<>();

            for (JsonNode actor : detail.path("actors")) {

                String name = actor.path("peopleNm").asString("");

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
                        .asString("");
            }

            return new Movie(
                    detail.path("movieCd").asString(movieCode),
                    detail.path("movieNm").asString(""),
                    detail.path("movieNmEn").asString(""),
                    detail.path("openDt").asString(""),
                    detail.path("showTm").asString(""),
                    genre,
                    director,
                    String.join(",", actors),
                    watchGrade);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "영화 상세정보 처리 실패",
                    e);
        }
    }

    // 외부 API 상세정보 보완

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
                movie.getDirector())
                .ifPresent(candidate -> applySupplement(movie, candidate));

        if (hasMissingDetails(movie)) {

            tmdbService.findMovieSupplement(
                    movie.getTitle(),
                    movie.getTitleEn(),
                    movie.getReleaseDate(),
                    movie.getDirector())
                    .ifPresent(candidate -> applySupplement(movie, candidate));
        }
    }

    // 줄거리는 기본 상세정보의 누락 여부와 별도로 조회한다.
    // DB에 저장하고 7일간 재시도하지 않아 외부 API 중복 호출을 막는다.
    private void supplementOverview(Movie movie) {
        if (!isBlank(movie.getOverview())
                || !movie.canAttemptOverview(LocalDateTime.now())) {
            return;
        }

        movie.markOverviewAttempted(LocalDateTime.now());
        tmdbService.findMovieOverview(
                movie.getTitle(),
                movie.getReleaseDate())
                .ifPresent(movie::updateOverview);
    }

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
                candidate.source());
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
                movie.getWatchGrade(),
                movie.getOverview());
    }

    // 박스오피스 캐시 변환

    private List<BoxOfficeResponse> readCachedBoxOffice(
            String contentJson) throws Exception {

        List<BoxOfficeResponse> movies = new ArrayList<>();

        JsonNode root = objectMapper.readTree(contentJson);
        if (!root.isArray())
            throw new IllegalStateException("박스오피스 캐시 구조가 배열이 아닙니다.");

        for (JsonNode movie : root) {
            if (isBlank(movie.path("movieCode").asString(""))
                    || isBlank(movie.path("title").asString(""))
                    || movie.path("rank").asInt(0) < 1) {
                throw new IllegalStateException("박스오피스 캐시 필수 필드가 누락되었습니다.");
            }
            movies.add(new BoxOfficeResponse(
                    movie.path("rank").asInt(),
                    movie.path("movieCode").asString(""),
                    movie.path("title").asString(""),
                    movie.path("releaseDate").asString(""),
                    movie.path("audienceCount").asString(""),
                    movie.path("totalAudience").asString(""),
                    movie.path("posterUrl").asString("")));
        }

        return movies;
    }

    // DB와 포스터 캐시를 이용한 포스터 조회

    private boolean isPosterUrlAvailable(String posterUrl) {
        if (isBlank(posterUrl)) {
            return false;
        }
        return !posterUrl.startsWith("/posters/")
                || posterFileStorageService.exists(posterUrl);
    }

    private boolean isMissingLocalPosterFile(String posterUrl) {
        return !isBlank(posterUrl)
                && posterUrl.startsWith("/posters/")
                && !posterFileStorageService.exists(posterUrl);
    }

    // DB의 URL뿐 아니라 로컬 파일의 실제 존재 여부까지 확인한다.
    private String findSavedPosterUrl(String movieCode) {
        if (isBlank(movieCode)) {
            return "";
        }

        Movie movie = movieRepository.findById(movieCode).orElse(null);
        String moviePosterUrl = movie == null ? "" : movie.getPosterUrl();

        if (isPosterUrlAvailable(moviePosterUrl)) {
            return moviePosterUrl;
        }

        MoviePosterCache posterCache = posterCacheService.findCache(movieCode).orElse(null);
        String cachedPosterUrl = posterCache != null
                && posterCache.getStatus() == PosterStatus.SUCCESS
                ? posterCache.getPosterUrl()
                : "";

        if (isPosterUrlAvailable(cachedPosterUrl)) {
            return cachedPosterUrl;
        }

        if (isMissingLocalPosterFile(moviePosterUrl)
                || isMissingLocalPosterFile(cachedPosterUrl)) {
            if (movie != null && isMissingLocalPosterFile(moviePosterUrl)) {
                movie.updatePosterUrl(null);
                movieRepository.save(movie);
            }

            posterCacheService.resetMissingPoster(
                    movieCode,
                    movie == null ? "" : movie.getTitle(),
                    movie == null ? "" : movie.getReleaseDate()
            );
        }

        return "";
    }

    // 포스터가 없는 영화만 비동기 검색

    private void enqueuePosterSearch(
            String movieCode,
            String title,
            String releaseDate) {

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
                    releaseDate,
                    100);

            String claimToken = posterCacheService.tryMarkAsProcessing(movieCode);

            if (claimToken == null) {
                return;
            }

            try {
                posterAsyncService.searchPosterAsync(
                        movieCode,
                        title,
                        releaseDate,
                        claimToken);
            } catch (RuntimeException schedulingException) {
                // 작업 등록에 실패한 영화는 즉시 재등록할 수 있도록 점유 상태를 해제한다.
                posterCacheService.releaseProcessingClaim(movieCode, claimToken);
                throw schedulingException;
            }

        } catch (Exception e) {

            log.error(
                    "포스터 검색 등록 실패. movieCode: {}",
                    movieCode,
                    e);
        }
    }

    // 박스오피스 포스터 캐시 보완

    private List<BoxOfficeResponse> enrichBoxOfficePosters(List<BoxOfficeResponse> movies) {
        if (movies == null || movies.isEmpty()) {
            return List.of();
        }

        List<String> codes = movies.stream()
                .filter(item -> item != null)
                .map(BoxOfficeResponse::movieCode)
                .filter(code -> !isBlank(code))
                .distinct()
                .toList();

        Map<String, Movie> savedMovies = new HashMap<>();
        movieRepository.findAllById(codes)
                .forEach(movie -> savedMovies.put(movie.getMovieCode(), movie));

        Map<String, MoviePosterCache> posterCaches = new HashMap<>();
        moviePosterCacheRepository.findAllByMovieCodeIn(codes)
                .forEach(cache -> posterCaches.put(cache.getMovieCode(), cache));

        List<BoxOfficeResponse> result = new ArrayList<>();
        for (BoxOfficeResponse item : movies) {
            String code = item.movieCode();
            Movie savedMovie = savedMovies.get(code);
            MoviePosterCache posterCache = posterCaches.get(code);

            String moviePosterUrl = savedMovie == null ? "" : savedMovie.getPosterUrl();
            String cachePosterUrl = posterCache != null
                    && posterCache.getStatus() == PosterStatus.SUCCESS
                    ? posterCache.getPosterUrl()
                    : "";
            String responsePosterUrl = item.posterUrl() == null ? "" : item.posterUrl();

            String posterUrl = isPosterUrlAvailable(moviePosterUrl) ? moviePosterUrl
                    : isPosterUrlAvailable(cachePosterUrl) ? cachePosterUrl
                    : isPosterUrlAvailable(responsePosterUrl) ? responsePosterUrl
                    : "";

            if (posterUrl.isBlank()) {
                boolean missingSavedFile = isMissingLocalPosterFile(moviePosterUrl)
                        || isMissingLocalPosterFile(cachePosterUrl);

                if (missingSavedFile) {
                    if (savedMovie != null && isMissingLocalPosterFile(moviePosterUrl)) {
                        savedMovie.updatePosterUrl(null);
                        movieRepository.save(savedMovie);
                    }

                    posterCacheService.resetMissingPoster(
                            code, item.title(), item.releaseDate());
                }

                enqueuePosterSearchWithoutLookup(code, item.title(), item.releaseDate(), 0);
            }

            result.add(new BoxOfficeResponse(
                    item.rank(),
                    code,
                    item.title(),
                    item.releaseDate(),
                    item.audienceCount(),
                    item.totalAudience(),
                    posterUrl
            ));
        }

        return result;
    }

    private void enqueuePosterSearchWithoutLookup(String movieCode, String title, String releaseDate, int priority) {
        if (isBlank(movieCode) || isBlank(title))
            return;
        try {
            posterCacheService.createCache(movieCode, title, releaseDate, priority);

            String claimToken = posterCacheService.tryMarkAsProcessing(movieCode);
            if (claimToken == null) {
                return;
            }

            try {
                posterAsyncService.searchBoxOfficePosterAsync(movieCode, title, releaseDate, claimToken);
            } catch (RuntimeException schedulingException) {
                // 박스오피스 경로도 스레드 풀 등록 실패 시 PROCESSING 점유를 즉시 해제한다.
                posterCacheService.releaseProcessingClaim(movieCode, claimToken);
                throw schedulingException;
            }
        } catch (Exception e) {
            log.error("박스오피스 포스터 검색 등록 실패. movieCode: {}", movieCode, e);
        }
    }

    // 일별 박스오피스 조회

    public List<BoxOfficeResponse> getDailyBoxOffice(
            String targetDate) {
        String lockKey = "DAILY_BOX_OFFICE|" + (targetDate == null ? "" : targetDate);
        Object lock = CACHE_LOCKS[Math.floorMod(lockKey.hashCode(), CACHE_LOCKS.length)];
        synchronized (lock) {
            return getDailyBoxOfficeInternal(targetDate);
        }
    }

    private List<BoxOfficeResponse> getDailyBoxOfficeInternal(String targetDate) {

        if (targetDate == null
                || !targetDate.matches("\\d{8}")) {
            throw new IllegalArgumentException(
                    "박스오피스 날짜는 YYYYMMDD 형식이어야 합니다.");
        }

        // 1. 날짜별 캐시 조회
        MovieCache cached = findValidCache(
                CACHE_DAILY_BOX_OFFICE,
                targetDate);

        if (cached != null) {

            try {

                List<BoxOfficeResponse> cachedMovies = readCachedBoxOffice(cached.getContentJson());
                markBoxOfficeHistory(cachedMovies, targetDate);

                return enrichBoxOfficePosters(cachedMovies);

            } catch (Exception e) {

                log.warn("박스오피스 캐시 변환 실패. KOBIS 재조회", e);
            }
        }

        // 2. 캐시가 없으면 KOBIS 호출
        log.info("KOBIS 일별 박스오피스 API 요청. targetDate: {}", targetDate);
        String json = kobisApiClient.getDailyBoxOffice(targetDate);

        JsonNode boxOfficeList = readRequiredArray(
                json,
                "boxOfficeResult",
                "dailyBoxOfficeList",
                "KOBIS 박스오피스 응답이 올바르지 않습니다.");

        List<BoxOfficeResponse> movies = new ArrayList<>();

        for (JsonNode item : boxOfficeList) {

            if (isBlank(item.path("movieCd").asString(""))
                    || isBlank(item.path("movieNm").asString(""))
                    || item.path("rank").asInt(0) < 1) {
                throw new IllegalStateException("KOBIS 박스오피스 항목의 필수 필드가 누락되었습니다.");
            }
            String movieCode = item.path("movieCd").asString("");
            String title = item.path("movieNm").asString("");
            String releaseDate = item.path("openDt").asString("");

            movies.add(new BoxOfficeResponse(
                    item.path("rank").asInt(),
                    movieCode,
                    title,
                    releaseDate,
                    item.path("audiCnt").asString(""),
                    item.path("audiAcc").asString(""),
                    ""));
        }

        // 3. 박스오피스 진입 이력을 영화 DB에 누적 저장
        markBoxOfficeHistory(movies, targetDate);

        // 4. 포스터 캐시를 확인하고 없는 영화만 비동기 검색
        List<BoxOfficeResponse> enriched = enrichBoxOfficePosters(movies);

        // 5. 날짜별 박스오피스 결과 캐시 저장
        saveCache(
                CACHE_DAILY_BOX_OFFICE,
                targetDate,
                enriched,
                DAILY_BOX_OFFICE_CACHE_HOURS);

        return enriched;
    }

    // 박스오피스 진입 이력 누적 저장

    private void markBoxOfficeHistory(
            List<BoxOfficeResponse> boxOfficeMovies,
            String targetDate) {
        if (boxOfficeMovies == null || boxOfficeMovies.isEmpty()) {
            return;
        }

        List<MovieSearchResponse> searchMovies = boxOfficeMovies.stream()
                .filter(item -> item != null)
                .map(item -> new MovieSearchResponse(
                        item.movieCode(),
                        item.title(),
                        item.releaseDate(),
                        "",
                        ""))
                .toList();

        // 박스오피스 영화가 Movie 테이블에 없으면 먼저 저장한다.
        saveSearchMoviesToDatabase(searchMovies);

        List<String> movieCodes = boxOfficeMovies.stream()
                .filter(item -> item != null)
                .map(item -> item == null ? null : item.movieCode())
                .filter(code -> !isBlank(code))
                .distinct()
                .toList();

        Map<String, Movie> savedMovies = new HashMap<>();
        movieRepository.findAllById(movieCodes)
                .forEach(movie -> savedMovies.put(movie.getMovieCode(), movie));

        for (BoxOfficeResponse item : boxOfficeMovies) {
            if (item == null || isBlank(item.movieCode())) {
                continue;
            }

            Movie movie = savedMovies.get(item.movieCode());
            if (movie != null) {
                movie.markBoxOfficeEntered(item.rank(), targetDate);
            }
        }

        movieRepository.saveAll(savedMovies.values());
    }

    // 최근 7일 박스오피스 이력 확보

    private void refreshRecentBoxOfficeHistory() {
        LocalDate today = LocalDate.now();

        // 오늘 기준 어제부터 7일 전까지의 일별 순위를 누적한다.
        for (int daysAgo = 1; daysAgo <= 7; daysAgo++) {
            String targetDate = today.minusDays(daysAgo)
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            try {
                getDailyBoxOffice(targetDate);
            } catch (Exception e) {
                // 특정 날짜의 API 응답이 실패해도 나머지 날짜는 계속 확인한다.
                log.warn("최근 박스오피스 이력 수집 실패. targetDate: {}",
                        targetDate, e);
            }
        }
    }

    // 박스오피스 이력이 확인된 추천 후보 조회

    public List<Map<String, Object>> getBoxOfficeRecommendations(String genre) {
        refreshRecentBoxOfficeHistory();

        String normalizedGenre = isBlank(genre)
                ? null
                : normalizeGenre(genre);

        if (normalizedGenre != null) {
            try {
                // 장르 목록을 한 번 수집해 박스오피스 영화의 장르 정보를 보완한다.
                getMoviesByGenre(normalizedGenre, 1);
            } catch (Exception e) {
                log.warn("추천용 장르 정보 보완 실패. genre: {}",
                        normalizedGenre, e);
            }
        }

        List<Movie> candidates = normalizedGenre == null
                ? movieRepository
                        .findByBoxOfficeEnteredTrueOrderByBestBoxOfficeRankAscLastBoxOfficeDateDesc()
                : movieRepository
                        .findByBoxOfficeEnteredTrueAndGenreContainingIgnoreCaseOrderByBestBoxOfficeRankAscLastBoxOfficeDateDesc(
                                normalizedGenre);

        // 개봉일 기준 최소 3개월이 지난 영화만 추천한다.
        String minimumReleaseDate = LocalDate.now()
                .minusMonths(3)
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        return candidates.stream()
                // 개봉일이 기준일보다 최근인 영화는 추천 후보에서 제외한다.
                .filter(movie -> movie.getReleaseDate() != null
                        && movie.getReleaseDate().matches("\\d{8}")
                        && movie.getReleaseDate().compareTo(minimumReleaseDate) <= 0)
                // 홈에 표시되는 최신 박스오피스 1위 영화와 추천작이 중복되지 않도록 제외한다.
                .filter(movie -> movie.getLastBoxOfficeRank() != null
                        && movie.getLastBoxOfficeRank() >= 2)
                .filter(movie -> movie.getMovieCode() != null
                        && movie.getMovieCode().matches("\\d{8}"))
                .filter(movie -> !isBlank(movie.getTitle()))
                // 개봉일이 확인되지 않는 항목은 추천 후보에서 제외한다.
                .filter(movie -> movie.getReleaseDate() != null
                        && movie.getReleaseDate().matches("\\d{8}"))
                .filter(movie -> normalizedGenre == null
                        || (!isBlank(movie.getGenre())
                                && movie.getGenre().toLowerCase()
                                        .contains(normalizedGenre.toLowerCase())))
                .limit(30)
                .map(movie -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("movieCode", movie.getMovieCode());
                    result.put("title", movie.getTitle());
                    result.put("releaseDate", movie.getReleaseDate());
                    result.put("genre", movie.getGenre());
                    result.put("director", movie.getDirector());
                    result.put("posterUrl", findSavedPosterUrl(movie.getMovieCode()));
                    result.put("bestBoxOfficeRank", movie.getBestBoxOfficeRank());
                    result.put("lastBoxOfficeDate", movie.getLastBoxOfficeDate());
                    result.put("lastBoxOfficeRank", movie.getLastBoxOfficeRank());
                    return result;
                })
                .toList();
    }

    // 장르 이름 정규화

    private String normalizeGenre(String genre) {

        return switch (genre.trim()) {
            case "로맨스" -> "멜로/로맨스";
            case "공포" -> "공포(호러)";
            default -> genre.trim();
        };
    }

    // 장르 전체 목록 수집

    private List<MovieSearchResponse> fetchAllMoviesByGenre(
            String genre) {

        List<MovieSearchResponse> result = new ArrayList<>();
        int totalPages = 1;

        for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
            log.info("KOBIS 장르 목록 API 요청. page: {}, pageSize: {}",
                    currentPage, GENRE_SCAN_SIZE);

            String json = kobisApiClient.searchMoviesByGenre(currentPage);
            JsonNode root;
            try {
                root = objectMapper.readTree(json);
            } catch (Exception exception) {
                throw new IllegalStateException("KOBIS 장르 목록 응답을 읽을 수 없습니다.", exception);
            }

            JsonNode totalCountNode = root.path("movieListResult").path("totCnt");
            if (!totalCountNode.canConvertToInt() || totalCountNode.asInt() < 0) {
                throw new IllegalStateException("KOBIS 장르 목록 총건수가 올바르지 않습니다.");
            }
            int totalCount = totalCountNode.asInt();
            totalPages = (int) Math.ceil((double) totalCount / GENRE_SCAN_SIZE);

            JsonNode movieList = readRequiredArray(json, "movieListResult", "movieList",
                    "KOBIS 장르 검색용 영화 목록 응답이 올바르지 않습니다.");
            if (movieList.isEmpty() && totalCount > 0) {
                throw new IllegalStateException("KOBIS 장르 목록이 비어 있지만 총건수는 0보다 큽니다.");
            }

            for (JsonNode movie : movieList) {
                String movieCode = movie.path("movieCd").asString("");
                String title = movie.path("movieNm").asString("");
                if (isBlank(movieCode) || isBlank(title)) {
                    throw new IllegalStateException("KOBIS 장르 목록에 영화 코드 또는 제목이 누락되었습니다.");
                }

                String movieGenre = movie.path("genreAlt").asString("");
                boolean matchesGenre = Arrays.stream(movieGenre.split(","))
                        .map(value -> value == null ? "" : value.trim())
                        .anyMatch(name -> name.equals(genre));
                if (!matchesGenre) {
                    continue;
                }

                String director = StreamSupport.stream(
                        movie.path("directors").spliterator(), false)
                        .map(node -> node.path("peopleNm").asString(""))
                        .filter(name -> !name.isBlank())
                        .distinct()
                        .limit(5)
                        .reduce((first, second) -> first + ", " + second)
                        .orElse("");

                result.add(new MovieSearchResponse(
                        movieCode,
                        title,
                        movie.path("openDt").asString(""),
                        movieGenre,
                        director));
            }
        }

        return result;
    }

    // 장르별 영화 조회

    public List<MovieSearchResponse> getMoviesByGenre(
            String genre,
            int page) {
        String lockKey = "GENRE|" + (genre == null ? "" : genre.trim());
        Object lock = CACHE_LOCKS[Math.floorMod(lockKey.hashCode(), CACHE_LOCKS.length)];
        synchronized (lock) {
            return getMoviesByGenreInternal(genre, page);
        }
    }

    private List<MovieSearchResponse> getMoviesByGenreInternal(String genre, int page) {

        if (isBlank(genre) || page < 1) {
            throw new IllegalArgumentException("장르와 페이지 번호를 확인해주세요.");
        }

        String normalizedGenre = normalizeGenre(genre);
        org.springframework.data.domain.Page<Movie> databasePage = movieRepository
                .findByGenreContainingIgnoreCaseOrderByReleaseDateDescMovieCodeAsc(
                        normalizedGenre,
                        org.springframework.data.domain.PageRequest.of(page - 1, GENRE_PAGE_SIZE));

        if (!databasePage.isEmpty()) {
            List<MovieSearchResponse> pageMovies = databasePage.getContent().stream()
                    .map(movie -> new MovieSearchResponse(
                            movie.getMovieCode(),
                            movie.getTitle(),
                            movie.getReleaseDate() == null ? "" : movie.getReleaseDate(),
                            movie.getGenre() == null ? "" : movie.getGenre(),
                            movie.getDirector() == null ? "" : movie.getDirector()))
                    .toList();
            enqueuePosterSearches(pageMovies);
            return pageMovies;
        }

        // DB에 해당 장르가 아직 없을 때만 KOBIS 전체 페이지를 조회해 초기 데이터를 채운다.
        if (page > 1) {
            return List.of();
        }

        List<MovieSearchResponse> allMovies = fetchAllMoviesByGenre(normalizedGenre);
        saveSearchMoviesToDatabase(allMovies);

        List<MovieSearchResponse> firstPage = allMovies.stream()
                .limit(GENRE_PAGE_SIZE)
                .toList();
        List<MovieSearchResponse> mergedPage = mergeSavedMovieDetails(firstPage);
        enqueuePosterSearches(mergedPage);
        return mergedPage;
    }

    // KOBIS 장르 목록의 누락 항목을 이미 저장된 상세정보로 보완한다.
    private List<MovieSearchResponse> mergeSavedMovieDetails(
            List<MovieSearchResponse> movies) {
        if (movies == null || movies.isEmpty()) {
            return List.of();
        }

        List<String> movieCodes = movies.stream()
                .map(item -> item == null ? null : item.movieCode())
                .filter(code -> !isBlank(code))
                .distinct()
                .toList();

        Map<String, Movie> savedMovies = new HashMap<>();
        movieRepository.findAllById(movieCodes)
                .forEach(movie -> savedMovies.put(movie.getMovieCode(), movie));

        return movies.stream()
                .map(item -> {
                    Movie savedMovie = savedMovies.get(item.movieCode());

                    if (savedMovie == null) {
                        return item;
                    }

                    return new MovieSearchResponse(
                            item.movieCode(),
                            item.title(),
                            firstNonBlank(item.releaseDate(), savedMovie.getReleaseDate()),
                            firstNonBlank(item.genre(), savedMovie.getGenre()),
                            firstNonBlank(item.director(), savedMovie.getDirector()));
                })
                .toList();
    }

    private String firstNonBlank(String primary, String fallback) {
        return !isBlank(primary) ? primary : (fallback == null ? "" : fallback);
    }

    // 포스터 조회 API

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
                    detail.releaseDate());

            return new MoviePosterResponse(movieCode, "");
        }

        // 3. 포스터 캐시를 확인하고 없을 때만 검색
        enqueuePosterSearch(
                movieCode,
                movie.getTitle(),
                movie.getReleaseDate());

        return new MoviePosterResponse(movieCode, "");
    }
}
