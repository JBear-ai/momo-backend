
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

    // =========================================================
    // 1. 영화 상세정보 보완
    // KMDb 보완 후에도 누락된 영화 정보를 TMDb에서 조회
    // =========================================================

    public Optional<MovieSupplement> findMovieSupplement(
            String title, String titleEn, String releaseDate, String kobisDirector) {

        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        try {
            String json = tmdbApiClient.searchMovie(title);

            if (json == null || json.isBlank()) {
                log.warn("TMDb 상세정보 검색 응답이 비어 있습니다. 제목: {}", title);
                return Optional.empty();
            }

            JsonNode results = objectMapper.readTree(json).path("results");

            if (!results.isArray() || results.isEmpty()) {
                return Optional.empty();
            }

            for (JsonNode result : results) {

                String candidateTitle = getText(result, "title");
                String candidateTitleEn = getText(result, "original_title");
                String candidateDate = getText(result, "release_date");

                // 검색된 영화의 제목과 원제를 KOBIS 정보와 비교
                String requestedTitle = normalizeTitle(title);
                String requestedTitleEn = normalizeTitle(titleEn);

                if (!requestedTitle.equals(normalizeTitle(candidateTitle))
                        && !requestedTitle.equals(normalizeTitle(candidateTitleEn))
                        && (requestedTitleEn.isBlank()
                        || (!requestedTitleEn.equals(normalizeTitle(candidateTitle))
                        && !requestedTitleEn.equals(normalizeTitle(candidateTitleEn))))) {
                    continue;
                }

                long movieId = result.path("id").asLong(0);

                if (movieId == 0) {
                    continue;
                }

                // 영화 ID로 상세정보 조회
                String detailJson = tmdbApiClient.getMovieDetail(movieId);

                if (detailJson == null || detailJson.isBlank()) {
                    continue;
                }

                JsonNode detail = objectMapper.readTree(detailJson);

                String director = "";
                List<String> actors = new ArrayList<>();

                // 감독 정보 추출
                for (JsonNode crew : detail.path("credits").path("crew")) {

                    if ("Director".equals(getText(crew, "job"))) {
                        director = getText(crew, "name");
                        break;
                    }
                }

                // 배우 최대 10명 추출
                for (JsonNode cast : detail.path("credits").path("cast")) {

                    String name = getText(cast, "name");

                    if (!name.isBlank()) {
                        actors.add(name);
                    }

                    if (actors.size() >= 10) {
                        break;
                    }
                }

                // 한국 극장 개봉일 추출
                String koreanReleaseDate = selectKoreanTheatricalReleaseDate(
                        detail.path("release_dates"));

                log.info(
                        "TMDb 개봉일 확인 - 영화: {}, 한국 개봉일: [{}]",
                        candidateTitle,
                        koreanReleaseDate
                );

                // KOBIS 개봉일이 있으면 검색 결과 개봉일을 우선 검증에 사용
                String verificationDate =
                        releaseDate == null || releaseDate.isBlank()
                                ? koreanReleaseDate
                                : candidateDate;

                // 동일 영화인지 최종 검증
                if (!MovieMatchVerifier.isSameMovie(
                        title,
                        titleEn,
                        releaseDate,
                        kobisDirector,
                        candidateTitle,
                        candidateTitleEn,
                        verificationDate,
                        director)) {
                    continue;
                }

                // 장르 추출
                String genre = StreamSupport.stream(
                                detail.path("genres").spliterator(), false)
                        .map(node -> getText(node, "name"))
                        .filter(name -> !name.isBlank())
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");

                // 검증된 영화의 보완 정보를 DTO로 반환
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
            log.warn("TMDb 상세정보 보완 실패 - 제목: {}", title, e);
        }

        return Optional.empty();
    }

    // =========================================================
    // 2. 한국 극장 개봉일 추출
    // =========================================================

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

    // =========================================================
    // 3. 영화 포스터 검색
    // =========================================================

    public String findMoviePosterUrl(
            String title,
            String releaseDate) {

        if (title == null || title.isBlank()) {
            log.warn("TMDb 검색 실패 - 영화 제목이 비어 있습니다.");
            return null;
        }

        try {

            String normalizedTitle = normalizeTitle(title);
            String normalizedDate = normalizeDate(releaseDate);

            log.info(
                    "TMDb API 요청 - 원본 제목: {}, 정규화 제목: {}, 개봉일: {}",
                    title, normalizedTitle, normalizedDate
            );

            String json = tmdbApiClient.searchMovie(title);

            if (json == null || json.isBlank()) {
                log.warn("TMDb API 응답이 비어 있습니다. 제목: {}", title);
                return null;
            }

            JsonNode results = objectMapper.readTree(json)
                    .path("results");

            if (!results.isArray() || results.isEmpty()) {
                log.warn(
                        "TMDb 검색 결과 없음 - 제목: {}, results: {}",
                        title, results
                );
                return null;
            }

            log.info(
                    "TMDb 검색 후보 개수: {}",
                    results.size()
            );

            String fallbackPosterUrl = null;

            for (JsonNode result : results) {

                String rawTitle = getText(result, "title");
                String rawOriginalTitle = getText(result, "original_title");
                String rawDate = getText(result, "release_date");
                String rawPosterPath = getText(result, "poster_path");

                String tmdbTitle = normalizeTitle(rawTitle);
                String originalTitle = normalizeTitle(rawOriginalTitle);

                log.info(
                        "TMDb 후보 - 제목: [{}], 원제: [{}], 개봉일: [{}], 포스터 경로: [{}]",
                        rawTitle,
                        rawOriginalTitle,
                        rawDate,
                        rawPosterPath
                );

                // 제목이 일치하지 않으면 제외
                if (!normalizedTitle.equals(tmdbTitle)
                        && !normalizedTitle.equals(originalTitle)) {

                    log.info(
                            "TMDb 제목 불일치 - 요청: [{}], 검색 제목: [{}], 원제: [{}]",
                            normalizedTitle, tmdbTitle, originalTitle
                    );

                    continue;
                }

                String posterUrl = extractPoster(result);

                if (posterUrl == null) {
                    log.info(
                            "TMDb 제목 일치했으나 포스터 경로 없음 - 제목: {}",
                            rawTitle
                    );
                    continue;
                }

                String tmdbReleaseDate = normalizeDate(rawDate);

                // 제목과 개봉일이 모두 일치하면 우선 반환
                if (!normalizedDate.isBlank()
                        && normalizedDate.equals(tmdbReleaseDate)) {

                    log.info(
                            "TMDb 영화 포스터 정확 매칭 성공 - 제목: {}, 개봉일: {}",
                            rawTitle, rawDate
                    );

                    return posterUrl;
                }

                // 제목이 정확히 일치하는 포스터를 보조 후보로 저장
                if (fallbackPosterUrl == null) {
                    fallbackPosterUrl = posterUrl;
                }
            }

            if (fallbackPosterUrl != null) {

                log.info(
                        "TMDb 영화 포스터 제목 매칭 성공 - 개봉일 차이 - 제목: {}",
                        title
                );

                return fallbackPosterUrl;
            }

            log.warn(
                    "TMDb 최종 검색 결과 없음 - 제목: {}, 개봉일: {}",
                    title, releaseDate
            );

            return null;

        } catch (Exception e) {

            log.error(
                    "TMDb 포스터 조회 중 예외 발생 - 제목: {}",
                    title,
                    e
            );

            return null;
        }
    }

    // =========================================================
    // 4. TV 프로그램 포스터 검색
    // =========================================================

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

    // =========================================================
    // 5. 공통 보조 메서드
    // =========================================================

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
                .toLowerCase();
    }
}
