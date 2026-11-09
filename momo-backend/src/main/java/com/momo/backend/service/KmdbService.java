
package com.momo.backend.service;

import com.momo.backend.client.KmdbApiClient;

import com.momo.backend.entity.MovieSupplement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KmdbService {

    private final KmdbApiClient kmdbApiClient;
    private final TmdbService tmdbService;
    private final ObjectMapper objectMapper;

    // KMDb에서 동일 영화를 찾아 KOBIS에 누락된 상세정보를 보완
    public Optional<MovieSupplement> findMovieSupplement(
            String title, String titleEn, String releaseDate, String director) {

        if ((title == null || title.isBlank())
                && (titleEn == null || titleEn.isBlank())) {
            return Optional.empty();
        }

        for (String searchTitle : searchTitles(title, titleEn)) {
            try {
                String json = kmdbApiClient.searchMovie(searchTitle);

                if (json == null || json.isBlank()) {
                    continue;
                }

                JsonNode dataList = objectMapper.readTree(json).path("Data");
                if (!dataList.isArray()) {
                    continue;
                }

                for (JsonNode data : dataList) {
                    for (JsonNode result : data.path("Result")) {
                        String candidateTitle = getText(result, "title");
                        String candidateTitleEn = firstNonBlank(
                                getText(result, "titleEng"),
                                getText(result, "titleOrg"));
                        String candidateReleaseDate = validDate(
                                getText(result, "repRlsDate"));
                        String candidateYear = getText(result, "prodYear");
                        String candidateDirector = String.join(",", nestedNames(
                                result.path("directors").path("director"),
                                "directorNm", 5));

                        if (!MovieMatchVerifier.isSameMovie(
                                title, titleEn, releaseDate, director,
                                candidateTitle, candidateTitleEn,
                                firstNonBlank(candidateReleaseDate, candidateYear),
                                candidateDirector)) {
                            continue;
                        }

                        List<String> actors = nestedNames(
                                result.path("actors").path("actor"),
                                "actorNm", 10);

                        return Optional.of(new MovieSupplement(
                                candidateTitleEn,
                                candidateReleaseDate,
                                getText(result, "runtime"),
                                getText(result, "genre"),
                                candidateDirector,
                                actors,
                                getText(result, "rating"),
                                "KMDb"
                        ));
                    }
                }
            } catch (Exception e) {
                // 한글 제목 검색이 실패해도 영문 제목 검색은 계속 진행한다.
                log.warn("KMDb 상세정보 검색 실패 - 검색어: {}", searchTitle, e);
            }
        }

        return Optional.empty();
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

    // 숫자 8자리 형식의 개봉일만 허용
    private String validDate(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        return digits.length() == 8 ? digits : "";
    }

    // 감독·배우 목록에서 지정한 개수만큼 이름 추출
    private List<String> nestedNames(JsonNode nodes, String fieldName, int limit) {
        List<String> names = new ArrayList<>();

        if (!nodes.isArray()) {
            return names;
        }

        for (JsonNode node : nodes) {
            String name = getText(node, fieldName);

            if (!name.isBlank()) {
                names.add(name);
            }

            if (names.size() >= limit) {
                break;
            }
        }

        return names;
    }

    // 여러 값 중 비어 있지 않은 첫 번째 값 반환
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    /**
     * 영화 코드, 제목, 개봉일로 포스터 검색
     *
     * 기존 호출부 호환을 위해 기존 함수명과 매개변수는 유지한다.
     */
    public String findPosterUrl(
            String movieCode,
            String title,
            String releaseDate) {
        return findPosterUrl(movieCode, title, "", releaseDate, "");
    }

    /**
     * 영화의 한글 제목, 영문 제목, 개봉일, 감독 정보를 활용해 포스터 검색
     */
    public String findPosterUrl(
            String movieCode,
            String title,
            String titleEn,
            String releaseDate,
            String director) {

        if ((title == null || title.isBlank())
                && (titleEn == null || titleEn.isBlank())) {
            log.warn("포스터 검색 실패 - 영화 제목이 비어 있습니다. movieCode: {}", movieCode);
            return null;
        }

        log.info("포스터 검색 요청 - 영화 코드: {}, 제목: {}, 영문 제목: {}, 개봉일: {}",
                movieCode, title, titleEn, releaseDate);

        String kmdbPosterUrl = findKmdbPosterUrl(
                title, titleEn, releaseDate, director);

        if (kmdbPosterUrl != null) {
            log.info("KMDb 포스터 검색 성공 - 영화 코드: {}", movieCode);
            return kmdbPosterUrl;
        }

        log.info("KMDb 포스터 검색 결과 없음. TMDb 검색으로 전환 - 영화 코드: {}", movieCode);

        String tmdbPosterUrl = tmdbService.findMoviePosterUrl(
                title, titleEn, releaseDate, director);

        if (tmdbPosterUrl != null && !tmdbPosterUrl.isBlank()) {
            log.info("TMDb 포스터 사용 - 영화 코드: {}", movieCode);
            return tmdbPosterUrl;
        }

        log.warn("포스터 검색 최종 실패 - 영화 코드: {}, 제목: {}, 개봉일: {}",
                movieCode, title, releaseDate);
        return null;
    }

    /**
     * 기존 메서드 호환용
     */
    public String findPosterUrl(String title, String releaseDate) {
        return findPosterUrl("", title, "", releaseDate, "");
    }

    /**
     * KMDb 포스터 검색
     */
    private String findKmdbPosterUrl(
            String title,
            String titleEn,
            String releaseDate,
            String director) {

        String bestPosterUrl = null;
        int bestMatchScore = -1;

        for (String searchTitle : searchTitles(title, titleEn)) {
            try {
                String json = kmdbApiClient.searchMovie(searchTitle);

                if (json == null || json.isBlank()) {
                    continue;
                }

                JsonNode dataList = objectMapper.readTree(json).path("Data");
                if (!dataList.isArray()) {
                    continue;
                }

                for (JsonNode data : dataList) {
                    JsonNode results = data.path("Result");
                    if (!results.isArray()) {
                        continue;
                    }

                    for (JsonNode result : results) {
                        String candidateTitle = getText(result, "title");
                        String candidateTitleEn = firstNonBlank(
                                getText(result, "titleEng"),
                                getText(result, "titleOrg"));
                        String candidateReleaseDate = validDate(
                                getText(result, "repRlsDate"));
                        String candidateYear = getText(result, "prodYear");
                        String candidateDirector = String.join(",", nestedNames(
                                result.path("directors").path("director"),
                                "directorNm", 5));

                        String verificationDate = firstNonBlank(
                                candidateReleaseDate, candidateYear);

                        // 제목·개봉연도 검증을 통과한 후보만 포스터 대상으로 인정한다.
                        if (!MovieMatchVerifier.isSameMovie(
                                title, titleEn, releaseDate, director,
                                candidateTitle, candidateTitleEn,
                                verificationDate, candidateDirector)) {
                            continue;
                        }

                        String posterUrl = extractPoster(result);
                        if (posterUrl == null) {
                            continue;
                        }

                        // 같은 영화 후보가 여러 개면 개봉일과 감독까지 더 정확히 일치하는 결과를 선택한다.
                        int matchScore = MovieMatchVerifier.matchScore(
                                title, titleEn, releaseDate, director,
                                candidateTitle, candidateTitleEn,
                                verificationDate, candidateDirector);

                        if (matchScore > bestMatchScore) {
                            bestMatchScore = matchScore;
                            bestPosterUrl = posterUrl;
                        }
                    }
                }
            } catch (Exception e) {
                // KMDb 한글 제목 검색에 실패해도 영문 제목 검색은 계속한다.
                log.warn("KMDb 포스터 검색 실패 - 검색어: {}", searchTitle, e);
            }
        }

        return bestPosterUrl;
    }

    /**
     * 포스터 URL 추출
     */
    private String extractPoster(JsonNode result) {

        String posters = getText(result, "posters");

        if (posters.isBlank()) {
            return null;
        }

        String[] posterUrls = posters.split("\\|");

        for (String url : posterUrls) {

            String trimmedUrl = url.trim();

            if (trimmedUrl.startsWith("https://")
                    || trimmedUrl.startsWith("http://")) {
                return trimmedUrl;
            }
        }

        return null;
    }

    /**
     * JSON 문자열 안전하게 가져오기
     */
    private String getText(JsonNode node, String fieldName) {

        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return "";
        }

        return value.asText("").trim();
    }

    /**
     * 개봉일 정규화
     */
    private String normalizeDate(String date) {

        if (date == null || date.isBlank()) {
            return "";
        }

        return date.replaceAll("[^0-9]", "");
    }

    /**
     * 영화 제목 정규화
     */
    private String normalizeTitle(String title) {

        if (title == null) {
            return "";
        }

        return title
                .replace("!HS", "")
                .replace("!HE", "")
                .replaceAll("\\s+", "")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .toLowerCase();
    }
}
