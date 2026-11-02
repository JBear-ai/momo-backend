
package com.momo.backend.service;

import com.momo.backend.client.TmdbApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class TmdbService {

    private final TmdbApiClient tmdbApiClient;
    private final ObjectMapper objectMapper;

    private static final String IMAGE_BASE_URL =
            "https://image.tmdb.org/t/p/w500";

    /**
     * 영화 포스터 검색
     */
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
                        "TMDb 후보 - 제목: [{}], 원제: [{}], " +
                                "개봉일: [{}], 포스터 경로: [{}]",
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

    /**
     * TV 프로그램 포스터 검색
     */
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

    /**
     * TMDb 포스터 주소 생성
     */
    private String extractPoster(JsonNode result) {

        String posterPath = getText(result, "poster_path");

        if (posterPath.isBlank() || !posterPath.startsWith("/")) {
            return null;
        }

        return IMAGE_BASE_URL + posterPath;
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
     * 제목 정규화
     */
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
