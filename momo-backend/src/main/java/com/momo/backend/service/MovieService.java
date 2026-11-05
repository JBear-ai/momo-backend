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
                    "페이지와 검색 개수는 1 이상이어야 합니다.");
        }

        String searchKeyword = keyword.trim()
                .replaceAll("\\s+", " ");

        String cacheKey = searchKeyword + "|" + page + "|" + size;

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
                size);

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
                    director));
        }

        // 3. 검색 결과를 DB에 저장
        saveSearchMoviesToDatabase(movies);

        // 검색 결과 전체의 포스터 수집 작업 등록
        enqueuePosterSearches(movies);

        boolean hasNext = (long) page * size < totalCount;

        return new MovieSearchPageResponse(
                movies,
                page,
                size,
                totalCount,
                hasNext);
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
                        releaseDate);

                // PENDING 상태인 경우에만 검색 작업 시작
                boolean started = posterCacheService
                        .tryMarkAsProcessing(movieCode);

                if (!started) {
                    continue;
                }

                posterAsyncService.searchPosterAsync(
                        movieCode,
                        title,
                        releaseDate);

            } catch (Exception e) {

                log.error(
                        "포스터 검색 등록 실패. movieCode: {}",
                        movieCode,
                        e);
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

        // 1. DB에 저장된 영화가 있는 경우
        if (savedMovie != null) {

            // 기존 영화의 누락 정보를 KMDb → TMDb 순서로 보완
            supplementMissingDetails(savedMovie);

            movieRepository.save(savedMovie);

            return toDetailResponse(savedMovie);
        }

        // DB에 영화 정보가 없을 때만 KOBIS API 호출
        String json = kobisApiClient.getMovieDetail(movieCode);

        try {

            JsonNode detail = objectMapper.readTree(json)
                    .path("movieInfoResult")
                    .path("movieInfo");

            // 감독 정보
            String director = "";

            JsonNode directors = detail.path("directors");

            if (directors.isArray() && !directors.isEmpty()) {

                director = directors.get(0)
                        .path("peopleNm")
                        .asText("");
            }

            String genre = StreamSupport.stream(
                    detail.path("genres").spliterator(),
                    false)
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
                    watchGrade);

            // KOBIS에 없는 상세정보를 KMDb → TMDb 순서로 보완
            supplementMissingDetails(newMovie);

            movieRepository.save(newMovie);

            return toDetailResponse(newMovie);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "영화 상세정보 처리 실패",
                    e);
        }
    }

    // =========================================================
    // 4. 영화 상세정보 누락 항목 보완
    // KMDb → TMDb 순서
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
                candidate.source());
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
                movie.getWatchGrade());
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

        // 페이지당 10개 영화
        int skipCount = (page - 1) * 10;
        int matchCount = 0;

        // KOBIS 원본 영화 목록을 최대 10페이지까지 탐색
        for (int currentPage = 1; currentPage <= 10; currentPage++) {

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

                    String movieCode = movie.path("movieCd").asText("");
                    String title = movie.path("movieNm").asText("");
                    String releaseDate = movie.path("openDt").asText("");

                    // KOBIS 목록에 개봉일이 없는 경우에만 보완 시도
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

        return result;
    }

    // =========================================================
    // 장르별 목록의 누락된 개봉일 보완 (추가 11-05)
    // =========================================================
    private String resolveReleaseDateForList(String movieCode) {

        if (isBlank(genre) || page < 1) {
            throw new IllegalArgumentException(
                    "장르와 페이지 번호를 확인해주세요.");
        }

        try {

            // 1. DB에 이미 보완된 영화 정보가 있다면 우선 사용
            Movie savedMovie = movieRepository.findById(movieCode)
                    .orElse(null);

            if (savedMovie != null
                    && !isBlank(savedMovie.getReleaseDate())) {

                return savedMovie.getReleaseDate();
            }

            // 2. DB에도 개봉일이 없다면 기존 상세정보 로직 재사용
            MovieDetailResponse detail = getMovieDetail(movieCode);

            if (detail != null
                    && !isBlank(detail.releaseDate())) {

                return detail.releaseDate();
            }

        } catch (Exception e) {

            log.warn(
                    "영화 목록 개봉일 보완 실패. movieCode: {}",
                    movieCode,
                    e);
        }

        // KOBIS / KMDb / TMDb 모두 개봉일이 없는 경우
        return "";
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

            MovieDetailResponse detail = getMovieDetail(movieCode);

            enqueuePosterSearch(
                    movieCode,
                    detail.title(),
                    detail.releaseDate());

            return new MoviePosterResponse(movieCode, "");
        }

        // 3. 캐시가 없다면 영화 상세정보 조회
        MovieDetailResponse movie = getMovieDetail(movieCode);

        // 4. 포스터 캐시 생성
        posterCacheService.createCache(
                movieCode,
                movie.title(),
                movie.releaseDate());

        // 5. PENDING 상태인 경우에만 검색 작업 시작
        boolean started = posterCacheService
                .tryMarkAsProcessing(movieCode);

        if (started) {

            // 6. 백그라운드 포스터 검색 실행
            posterAsyncService.searchPosterAsync(
                    movieCode,
                    movie.title(),
                    movie.releaseDate());
        }

        // 7. 포스터 검색 완료를 기다리지 않고 즉시 반환
        return new MoviePosterResponse(movieCode, "");
    }
}
