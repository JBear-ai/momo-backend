
package com.momo.backend.service;

import com.momo.backend.client.TmdbApiClient;

import com.momo.backend.entity.MovieSupplement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmdbService {

    private final TmdbApiClient tmdbApiClient;
    private final ObjectMapper objectMapper;

    private static final String IMAGE_BASE_URL =
            "https://image.tmdb.org/t/p/w500";

    // 1. 영화 상세정보 보완
    // KMDb에서 누락된 정보가 있을 때 TMDb에서 추가로 조회한다.
    public Optional<MovieSupplement> findMovieSupplement(
            String title, String titleEn, String releaseDate, String kobisDirector) {

        if ((title == null || title.isBlank())
                && (titleEn == null || titleEn.isBlank())) {
            return Optional.empty();
        }

        for (String searchTitle : searchTitles(title, titleEn)) {
            try {
                String json = searchMovie(searchTitle, titleEn);
                if (json == null || json.isBlank()) {
                    continue;
                }

                JsonNode results = objectMapper.readTree(json).path("results");
                if (!results.isArray()) {
                    continue;
                }

                for (JsonNode result : results) {
                    String candidateTitle = getText(result, "title");
                    String candidateTitleEn = getText(result, "original_title");

                    if (!matchesTitle(title, titleEn, candidateTitle, candidateTitleEn)) {
                        continue;
                    }

                    long movieId = result.path("id").asLong(0);
                    if (movieId <= 0) {
                        continue;
                    }

                    String detailJson = tmdbApiClient.getMovieDetail(movieId);
                    if (detailJson == null || detailJson.isBlank()) {
                        continue;
                    }

                    JsonNode detail = objectMapper.readTree(detailJson);
                    List<String> directors = new ArrayList<>();
                    List<String> actors = new ArrayList<>();

                    // 공동 감독이 있는 작품도 누락하지 않도록 모든 감독을 수집한다.
                    for (JsonNode crew : detail.path("credits").path("crew")) {
                        if (!"Director".equalsIgnoreCase(getText(crew, "job"))) {
                            continue;
                        }

                        String name = getText(crew, "name");
                        if (!name.isBlank() && !directors.contains(name)) {
                            directors.add(name);
                        }

                        if (directors.size() >= 5) {
                            break;
                        }
                    }

                    String director = String.join(", ", directors);

                    for (JsonNode cast : detail.path("credits").path("cast")) {
                        String name = getText(cast, "name");
                        if (!name.isBlank()) {
                            actors.add(name);
                        }
                        if (actors.size() >= 10) {
                            break;
                        }
                    }

                    String koreanReleaseDate = selectKoreanTheatricalReleaseDate(
                            detail.path("release_dates"));
                    String candidateDate = getText(result, "release_date");

                    // 국내 개봉일이 있으면 해외 최초 개봉일보다 국내 개봉일을 우선 검증한다.
                    String verificationDate = firstNonBlank(
                            koreanReleaseDate, candidateDate);

                    if (!MovieMatchVerifier.isSameMovie(
                            title, titleEn, releaseDate, kobisDirector,
                            candidateTitle, candidateTitleEn,
                            verificationDate, director)) {
                        continue;
                    }

                    String genre = StreamSupport.stream(
                                    detail.path("genres").spliterator(), false)
                            .map(node -> getText(node, "name"))
                            .filter(name -> !name.isBlank())
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");

                    return Optional.of(new MovieSupplement(
                            candidateTitleEn,
                            koreanReleaseDate,
                            detail.path("runtime").asInt(0) > 0
                                    ? detail.path("runtime").asText()
                                    : "",
                            genre,
                            director,
                            actors,
                            "",
                            "TMDb"
                    ));
                }
            } catch (Exception e) {
                // 한글 제목 검색이 실패해도 영문 제목 검색은 계속 진행한다.
                log.warn("TMDb 상세정보 검색 실패 - 검색어: {}", searchTitle, e);
            }
        }

        return Optional.empty();
    }

    // 2. 한국 극장 개봉일 추출

    // 한국(KR)의 일반 극장 개봉을 우선하고, 없으면 제한 개봉일 사용
    static String selectKoreanTheatricalReleaseDate(JsonNode releaseDates) {

        String limited = "";

        for (JsonNode country : releaseDates.path("results")) {

            if (!"KR".equals(getTextStatic(country, "iso_3166_1"))) {
                continue;
            }

            for (JsonNode item : country.path("release_dates")) {

                String date = normalizeTmdbDate(
                        getTextStatic(item, "release_date"));

                if (date.isBlank()) {
                    continue;
                }

                int type = item.path("type").asInt(0);

                // 일반 극장 개봉
                if (type == 3) {
                    return date;
                }

                // 제한 개봉
                if (type == 2 && limited.isBlank()) {
                    limited = date;
                }
            }
        }

        return limited;
    }

    // TMDb 날짜에서 YYYYMMDD 형식 추출
    private static String normalizeTmdbDate(String value) {

        String digits = value == null
                ? ""
                : value.replaceAll("[^0-9]", "");

        return digits.length() >= 8
                ? digits.substring(0, 8)
                : "";
    }

    // JSON 필드가 없거나 null이면 빈 문자열 반환
    private static String getTextStatic(JsonNode node, String fieldName) {

        JsonNode value = node.path(fieldName);

        return value.isMissingNode() || value.isNull()
                ? ""
                : value.asText("").trim();
    }

    // 3. 영화 포스터 검색

    // 기존 호출부 호환을 위해 기존 메서드명과 매개변수를 유지한다.
    public String findMoviePosterUrl(String title, String releaseDate) {
        return findMoviePosterUrl(title, "", releaseDate, "");
    }

    // 한글 제목, 영문 제목, 개봉일, 감독을 함께 사용해 포스터 후보를 검증한다.
    public String findMoviePosterUrl(
            String title,
            String titleEn,
            String releaseDate,
            String kobisDirector) {

        if ((title == null || title.isBlank())
                && (titleEn == null || titleEn.isBlank())) {
            return null;
        }

        String bestPosterUrl = null;
        int bestMatchScore = -1;
        final int maxDetailChecksPerLanguage = 5;

        for (String searchTitle : searchTitles(title, titleEn)) {
            int detailChecks = 0;
            try {
                String json = searchMovie(searchTitle, titleEn);
                if (json == null || json.isBlank()) {
                    continue;
                }

                JsonNode results = objectMapper.readTree(json).path("results");
                if (!results.isArray()) {
                    continue;
                }

                for (JsonNode result : results) {
                    String candidateTitle = getText(result, "title");
                    String candidateOriginalTitle = getText(result, "original_title");

                    if (!matchesTitle(title, titleEn, candidateTitle, candidateOriginalTitle)) {
                        continue;
                    }

                    String posterUrl = extractPoster(result);
                    if (posterUrl == null) {
                        continue;
                    }

                    String candidateDate = getText(result, "release_date");
                    String verificationDate = candidateDate;
                    String candidateDirector = "";

                    // 상위 후보의 국내 개봉일과 감독을 확인해 동명 영화 오매칭을 줄인다.
                    if (detailChecks < maxDetailChecksPerLanguage) {
                        detailChecks++;

                        long movieId = result.path("id").asLong(0);
                        if (movieId > 0) {
                            try {
                                String detailJson = tmdbApiClient.getMovieDetail(movieId);

                                if (detailJson != null && !detailJson.isBlank()) {
                                    JsonNode detail = objectMapper.readTree(detailJson);

                                    String koreanReleaseDate =
                                            selectKoreanTheatricalReleaseDate(
                                                    detail.path("release_dates"));

                                    if (!koreanReleaseDate.isBlank()) {
                                        verificationDate = koreanReleaseDate;
                                    }

                                    List<String> directors = new ArrayList<>();

                                    for (JsonNode crew : detail.path("credits").path("crew")) {
                                        if (!"Director".equalsIgnoreCase(getText(crew, "job"))) {
                                            continue;
                                        }

                                        String name = getText(crew, "name");
                                        if (!name.isBlank() && !directors.contains(name)) {
                                            directors.add(name);
                                        }

                                        if (directors.size() >= 5) {
                                            break;
                                        }
                                    }

                                    candidateDirector = String.join(", ", directors);
                                }
                            } catch (Exception e) {
                                // 상세정보 조회가 실패해도 검색 결과의 개봉연도로 검증을 계속한다.
                                log.debug("TMDb 포스터 후보 상세 검증 실패 - ID: {}", movieId, e);
                            }
                        }
                    }

                    if (!MovieMatchVerifier.isSameMovie(
                            title, titleEn, releaseDate, kobisDirector,
                            candidateTitle, candidateOriginalTitle,
                            verificationDate, candidateDirector)) {
                        continue;
                    }

                    int matchScore = MovieMatchVerifier.matchScore(
                            title, titleEn, releaseDate, kobisDirector,
                            candidateTitle, candidateOriginalTitle,
                            verificationDate, candidateDirector);

                    if (matchScore > bestMatchScore) {
                        bestMatchScore = matchScore;
                        bestPosterUrl = posterUrl;
                    }
                }
            } catch (Exception e) {
                // 한글 제목 검색이 실패해도 영문 제목 검색은 계속한다.
                log.warn("TMDb 포스터 검색 실패 - 검색어: {}", searchTitle, e);
            }
        }

        if (bestPosterUrl != null) {
            log.info("TMDb 영화 포스터 최종 후보 선택 - 제목: {}, 개봉일: {}, 일치 점수: {}",
                    title, releaseDate, bestMatchScore);
            return bestPosterUrl;
        }

        log.warn("TMDb 포스터 검색 결과 없음 - 제목: {}, 영문 제목: {}, 개봉일: {}",
                title, titleEn, releaseDate);
        return null;
    }

    // 4. 영화 줄거리 조회

    public Optional<String> findMovieOverview(String title, String releaseDate) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        try {
            String json = tmdbApiClient.searchMovie(title);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }

            JsonNode results = objectMapper.readTree(json).path("results");
            if (!results.isArray() || results.isEmpty()) {
                return Optional.empty();
            }

            String requestedTitle = normalizeTitle(title);
            String requestedYear = releaseDate != null && releaseDate.length() >= 4
                    ? releaseDate.substring(0, 4) : "";

            for (JsonNode result : results) {
                String candidateTitle = normalizeTitle(getText(result, "title"));
                String candidateOriginalTitle = normalizeTitle(getText(result, "original_title"));

                if (!requestedTitle.equals(candidateTitle)
                        && !requestedTitle.equals(candidateOriginalTitle)) {
                    continue;
                }

                String candidateDate = getText(result, "release_date");
                if (!requestedYear.isBlank()
                        && (candidateDate.length() < 4
                        || !requestedYear.equals(candidateDate.substring(0, 4)))) {
                    continue;
                }

                long movieId = result.path("id").asLong(0);
                if (movieId <= 0) {
                    continue;
                }

                String detailJson = tmdbApiClient.getMovieDetail(movieId);
                if (detailJson == null || detailJson.isBlank()) {
                    continue;
                }

                String overview = getText(
                        objectMapper.readTree(detailJson), "overview");
                if (!overview.isBlank()) {
                    return Optional.of(overview);
                }
            }
        } catch (Exception e) {
            log.warn("TMDb 영화 줄거리 조회 실패 - 제목: {}", title, e);
        }

        return Optional.empty();
    }

    // 5. TV 프로그램 포스터 검색

    public String findTvPosterUrl(String title) {

        if (title == null || title.isBlank()) {
            return null;
        }

        try {

            String normalizedTitle = normalizeTitle(title);

            String json = tmdbApiClient.searchTv(title);

            if (json == null || json.isBlank()) {
                return null;
            }

            JsonNode results = objectMapper.readTree(json)
                    .path("results");

            if (!results.isArray() || results.isEmpty()) {
                return null;
            }

            for (JsonNode result : results) {

                String tmdbTitle = normalizeTitle(
                        getText(result, "name")
                );

                String originalTitle = normalizeTitle(
                        getText(result, "original_name")
                );

                if (!normalizedTitle.equals(tmdbTitle)
                        && !normalizedTitle.equals(originalTitle)) {
                    continue;
                }

                String posterUrl = extractPoster(result);

                if (posterUrl != null) {

                    log.info(
                            "TMDb TV 포스터 검색 성공 - 제목: {}",
                            title
                    );

                    return posterUrl;
                }
            }

            log.info("TMDb TV 포스터 검색 결과 없음 - 제목: {}", title);

            return null;

        } catch (Exception e) {

            log.warn(
                    "TMDb TV 포스터 조회 실패 - 제목: {}",
                    title,
                    e
            );

            return null;
        }
    }

    // 한글 제목은 한국어, 영문 제목은 영어 검색 결과로 조회한다.
    private String searchMovie(String searchTitle, String titleEn) {
        String language = !normalizeTitle(titleEn).isBlank()
                && normalizeTitle(searchTitle).equals(normalizeTitle(titleEn))
                ? "en-US"
                : "ko-KR";

        return tmdbApiClient.searchMovie(searchTitle, language);
    }

    // 한글 제목과 영문 제목을 중복 없이 검색어로 사용
    private List<String> searchTitles(String title, String titleEn) {
        List<String> titles = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            titles.add(title.trim());
        }

        if (titleEn != null && !titleEn.isBlank()
                && titles.stream().noneMatch(value ->
                normalizeTitle(value).equals(normalizeTitle(titleEn)))) {
            titles.add(titleEn.trim());
        }

        return titles;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean matchesTitle(
            String title,
            String titleEn,
            String candidateTitle,
            String candidateOriginalTitle) {

        String requestedTitle = normalizeTitle(title);
        String requestedTitleEn = normalizeTitle(titleEn);
        String foundTitle = normalizeTitle(candidateTitle);
        String foundOriginalTitle = normalizeTitle(candidateOriginalTitle);

        return (!requestedTitle.isBlank()
                && (requestedTitle.equals(foundTitle)
                || requestedTitle.equals(foundOriginalTitle)))
                || (!requestedTitleEn.isBlank()
                && (requestedTitleEn.equals(foundTitle)
                || requestedTitleEn.equals(foundOriginalTitle)));
    }

    private String releaseYear(String date) {
        String digits = normalizeDate(date);
        return digits.length() >= 4 ? digits.substring(0, 4) : "";
    }

    // 6. 공통 보조 메서드

    // TMDb 포스터 주소 생성
    private String extractPoster(JsonNode result) {

        String posterPath = getText(result, "poster_path");

        if (posterPath.isBlank() || !posterPath.startsWith("/")) {
            return null;
        }

        return IMAGE_BASE_URL + posterPath;
    }

    // JSON 문자열 안전하게 가져오기
    private String getText(JsonNode node, String fieldName) {

        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return "";
        }

        return value.asText("").trim();
    }

    // 개봉일 정규화
    private String normalizeDate(String date) {

        if (date == null || date.isBlank()) {
            return "";
        }

        return date.replaceAll("[^0-9]", "");
    }

    // 제목 정규화
    private String normalizeTitle(String title) {

        if (title == null) {
            return "";
        }

        return title
                .replaceAll("\\s+", "")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .toLowerCase(java.util.Locale.ROOT);
    }
}
