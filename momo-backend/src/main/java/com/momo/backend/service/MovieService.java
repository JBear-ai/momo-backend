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
    private final MovieCacheRepository movieCacheRepository;
    private final MoviePosterCacheService posterCacheService;

    // =========================================================
    // 통합 캐시 설정
    // =========================================================

    private static final String CACHE_SEARCH = "SEARCH";
    private static final String CACHE_DAILY_BOX_OFFICE = "DAILY_BOX_OFFICE";
    private static final String CACHE_GENRE = "GENRE";

    private static final long SEARCH_CACHE_HOURS = 24;
    private static final long DAILY_BOX_OFFICE_CACHE_HOURS = 1;
    private static final long GENRE_CACHE_HOURS = 24;

    // =========================================================
    // 1. 통합 캐시 조회
    // =========================================================

    private MovieCache findValidCache(
            String cacheType,
            String cacheKey
    ) {

        MovieCache cache = movieCacheRepository
                .findByCacheTypeAndCacheKey(cacheType, cacheKey)
                .orElse(null);

        if (cache == null) {
            return null;
        }

        if (!cache.isValid(LocalDateTime.now())) {
            return null;
        }

        return cache;
    }

    // =========================================================
    // 2. 통합 캐시 저장 및 갱신
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

            // 캐시 저장 실패가 API 응답 자체를 중단시키지 않도록 처리
            log.warn(
                    "영화 통합 캐시 저장 실패. type: {}, key: {}",
                    cacheType,
                    cacheKey,
                    e
            );
        }
    }

    // =========================================================
    // 3. 영화 검색 캐시 JSON 변환
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
    // 4. 영화 검색 (통합 캐시 적용)
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

        // 검색어 앞뒤 공백 및 중복 공백 제거
        String[] words = keyword.trim().split("\\s+");

        List<String> keywords = new ArrayList<>();

        for (String word : words) {
            if (!keywords.contains(word)) {
                keywords.add(word);
            }
        }

        String searchKeyword = String.join(" ", keywords);

        // 검색어 + 페이지 + 개수를 캐시 식별값으로 사용
        String cacheKey = searchKeyword + "|" + page + "|" + size;

        // 1. 통합 캐시 조회
        MovieCache cached = findValidCache(
                CACHE_SEARCH,
                cacheKey
        );

        if (cached != null) {

            try {

                MovieSearchPageResponse response =
                        readCachedSearchPage(cached.getContentJson());

                // 캐시 결과에서도 포스터 누락 작업 등록
                enqueuePosterSearches(response.content());

                return response;

            } catch (Exception e) {

                log.warn("영화 검색 캐시 변환 실패. KOBIS 재조회 진행", e);
            }
        }

        // 2. 캐시가 없거나 만료되면 KOBIS 호출
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

        // 3. 포스터 비동기 검색 등록
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

        // 4. 통합 캐시에 저장
        saveCache(
                CACHE_SEARCH,
                cacheKey,
                response,
                SEARCH_CACHE_HOURS
        );

        return response;
    }

    // =========================================================
    // 5. 검색 및 장르별 영화의 포스터 비동기 등록
    // =========================================================

    private void enqueuePosterSearches(
            List<MovieSearchResponse> movies
    ) {

        for (MovieSearchResponse movie : movies) {

            String movieCode = movie.movieCode();
            String title = movie.title();
            String releaseDate = movie.releaseDate();

            if (movieCode == null || movieCode.isBlank()
                    || title == null || title.isBlank()) {
                continue;
            }

            try {

                posterCacheService.createCache(
                        movieCode,
                        title,
                        releaseDate
                );

                boolean started = posterCacheService
                        .tryMarkAsProcessing(movieCode);

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
                        "포스터 검색 작업 등록 실패. movieCode: {}",
                        movieCode,
                        e
                );
            }
        }
    }

    // =========================================================
    // 6. 영화 상세정보 조회
    // =========================================================

    @Transactional
    public MovieDetailResponse getMovieDetail(String movieCode) {

        Movie savedMovie = movieRepository.findById(movieCode)
                .orElse(null);

        if (savedMovie != null) {

            supplementMissingDetails(savedMovie);

            movieRepository.save(savedMovie);

            return toDetailResponse(savedMovie);
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
    // 7. 영화 상세정보 누락 항목 보완
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

    // =========================================================
    // 8. 외부 API 보완 정보를 Movie 엔티티에 적용
    // =========================================================

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

    // =========================================================
    // 9. 영화 상세정보 누락 여부 확인
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // =========================================================
    // 10. Movie 엔티티를 응답 DTO로 변환
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
    // 11. 박스오피스 캐시 JSON 변환
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
    // 12. 일별 박스오피스 조회
    // =========================================================

    public List<BoxOfficeResponse> getDailyBoxOffice(
            String targetDate
    ) {

        if (targetDate == null || !targetDate.matches("\\d{8}")) {
            throw new IllegalArgumentException(
                    "박스오피스 조회 날짜는 YYYYMMDD 형식이어야 합니다."
            );
        }

        // 날짜를 캐시 식별값으로 사용
        String cacheKey = targetDate;

        // 1. 통합 캐시 조회
        MovieCache cached = findValidCache(
                CACHE_DAILY_BOX_OFFICE,
                cacheKey
        );

        if (cached != null) {

            try {

                return readCachedBoxOffice(cached.getContentJson());

            } catch (Exception e) {

                log.warn("박스오피스 캐시 변환 실패. KOBIS 재조회 진행", e);
            }
        }

        // 2. 캐시가 없거나 만료되면 KOBIS 호출
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

        // 3. 포스터가 포함된 최종 결과 저장
        saveCache(
                CACHE_DAILY_BOX_OFFICE,
                cacheKey,
                movies,
                DAILY_BOX_OFFICE_CACHE_HOURS
        );

        return movies;
    }

    // =========================================================
    // 13. 장르별 영화 캐시 JSON 변환
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
    // 14. 장르별 영화 목록 조회
    // =========================================================

    public List<MovieSearchResponse> getMoviesByGenre(
            String genre,
            int page
    ) {

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

        // 장르 + 페이지를 캐시 식별값으로 사용
        String cacheKey = kobisGenre + "|" + page;

        // 1. 통합 캐시 조회
        MovieCache cached = findValidCache(
                CACHE_GENRE,
                cacheKey
        );

        if (cached != null) {

            try {

                List<MovieSearchResponse> cachedMovies =
                        readCachedGenreMovies(cached.getContentJson());

                enqueuePosterSearches(cachedMovies);

                return cachedMovies;

            } catch (Exception e) {

                log.warn("장르별 영화 캐시 변환 실패. KOBIS 재조회 진행", e);
            }
        }

        // 2. 캐시가 없거나 만료되면 KOBIS 조회
        List<MovieSearchResponse> movies = new ArrayList<>();

        int skipCount = (page - 1) * 10;
        int matchCount = 0;

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

                    String movieCode = movie.path("movieCd").asText("");
                    String title = movie.path("movieNm").asText("");
                    String releaseDate = movie.path("openDt").asText("");

                    if (isBlank(releaseDate)) {
                        releaseDate = resolveReleaseDateForList(movieCode);
                    }

                    movies.add(new MovieSearchResponse(
                            movieCode,
                            title,
                            releaseDate,
                            movieGenre,
                            director
                    ));

                    if (movies.size() >= 10) {
                        break;
                    }
                }

                if (movies.size() >= 10) {
                    break;
                }

            } catch (Exception e) {

                throw new IllegalStateException(
                        "장르별 영화 목록을 처리하지 못했습니다.",
                        e
                );
            }
        }

        // 3. 포스터 비동기 검색 등록
        enqueuePosterSearches(movies);

        // 4. 장르별 결과 통합 캐시 저장
        saveCache(
                CACHE_GENRE,
                cacheKey,
                movies,
                GENRE_CACHE_HOURS
        );

        return movies;
    }

    // =========================================================
    // 15. 장르별 목록의 누락된 개봉일 보완
    // =========================================================

    private String resolveReleaseDateForList(String movieCode) {

        if (movieCode == null || movieCode.isBlank()) {
            return "";
        }

        try {

            Movie savedMovie = movieRepository.findById(movieCode)
                    .orElse(null);

            if (savedMovie != null
                    && !isBlank(savedMovie.getReleaseDate())) {

                return savedMovie.getReleaseDate();
            }

            MovieDetailResponse detail = getMovieDetail(movieCode);

            if (detail != null
                    && !isBlank(detail.releaseDate())) {

                return detail.releaseDate();
            }

        } catch (Exception e) {

            log.warn(
                    "영화 목록 개봉일 보완 실패. movieCode: {}",
                    movieCode,
                    e
            );
        }

        return "";
    }

    // =========================================================
    // 16. 영화 코드로 포스터 조회 및 비동기 캐시 관리
    // =========================================================

    public MoviePosterResponse getMoviePoster(String movieCode) {

        MoviePosterCache savedCache = posterCacheService
                .findCache(movieCode)
                .orElse(null);

        if (savedCache != null) {

            if (savedCache.getStatus() == PosterStatus.SUCCESS) {

                return new MoviePosterResponse(
                        movieCode,
                        savedCache.getPosterUrl() == null
                                ? ""
                                : savedCache.getPosterUrl()
                );
            }

            return new MoviePosterResponse(movieCode, "");
        }

        MovieDetailResponse movie = getMovieDetail(movieCode);

        posterCacheService.createCache(
                movieCode,
                movie.title(),
                movie.releaseDate()
        );

        boolean started = posterCacheService
                .tryMarkAsProcessing(movieCode);

        if (started) {

            posterAsyncService.searchPosterAsync(
                    movieCode,
                    movie.title(),
                    movie.releaseDate()
            );
        }

        return new MoviePosterResponse(movieCode, "");
    }
}