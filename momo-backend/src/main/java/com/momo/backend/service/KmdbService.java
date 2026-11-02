
package com.momo.backend.service;

import com.momo.backend.client.KmdbApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class KmdbService {

    private final KmdbApiClient kmdbApiClient;
    private final TmdbService tmdbService;
    private final ObjectMapper objectMapper;

    /**
     * 영화 코드, 제목, 개봉일로 포스터 검색
     *
     * 1. KMDb 검색
     * 2. TMDB 검색
     * 3. 모두 실패하면 null 반환
     */
    public String findPosterUrl(
            String movieCode,
            String title,
            String releaseDate) {

        if (title == null || title.isBlank()) {

            log.info("포스터 검색 실패 - 영화 제목이 비어 있습니다.");

            return null;
        }

        // 1. KMDb 검색
        String kmdbPosterUrl = findKmdbPosterUrl(
                title,
                releaseDate
        );

        if (kmdbPosterUrl != null) {
            return kmdbPosterUrl;
        }

        // 2. TMDB 검색
        String tmdbPosterUrl = tmdbService.findMoviePosterUrl(
                title,
                releaseDate
        );

        if (tmdbPosterUrl != null) {

            log.info(
                    "TMDB 포스터 사용 - 영화 코드: {}, 제목: {}",
                    movieCode,
                    title
            );

            return tmdbPosterUrl;
        }

        // 3. 포스터 검색 실패
        log.info(
                "포스터 검색 결과 없음 - 영화 코드: {}, 제목: {}",
                movieCode,
                title
        );

        return null;
    }

    /**
     * 기존 메서드 호환용
     */
    public String findPosterUrl(
            String title,
            String releaseDate) {

        String kmdbPosterUrl = findKmdbPosterUrl(
                title,
                releaseDate
        );

        if (kmdbPosterUrl != null) {
            return kmdbPosterUrl;
        }

        return tmdbService.findMoviePosterUrl(
                title,
                releaseDate
        );
    }

    /**
     * KMDb 포스터 검색
     */
    private String findKmdbPosterUrl(
            String title,
            String releaseDate) {

        if (title == null || title.isBlank()) {
            return null;
        }

        try {

            String normalizedTitle = normalizeTitle(title);
            String normalizedDate = normalizeDate(releaseDate);

            String json = kmdbApiClient.searchMovie(title);

            if (json == null || json.isBlank()) {
                return null;
            }

            JsonNode dataList = objectMapper.readTree(json)
                    .path("Data");

            if (!dataList.isArray() || dataList.isEmpty()) {
                return null;
            }

            String fallbackPosterUrl = null;

            for (JsonNode data : dataList) {

                JsonNode results = data.path("Result");

                if (!results.isArray() || results.isEmpty()) {
                    continue;
                }

                for (JsonNode result : results) {

                    String kmdbTitle = normalizeTitle(
                            getText(result, "title")
                    );

                    String kmdbReleaseDate = normalizeDate(
                            getText(result, "repRlsDate")
                    );

                    if (!normalizedTitle.equals(kmdbTitle)) {
                        continue;
                    }

                    String posterUrl = extractPoster(result);

                    if (posterUrl == null) {
                        continue;
                    }

                    if (!normalizedDate.isBlank()
                            && normalizedDate.equals(kmdbReleaseDate)) {

                        log.info(
                                "KMDb 포스터 정확 매칭 - 제목: {}, 개봉일: {}",
                                title,
                                releaseDate
                        );

                        return posterUrl;
                    }

                    if (fallbackPosterUrl == null) {
                        fallbackPosterUrl = posterUrl;
                    }
                }
            }

            if (fallbackPosterUrl != null) {

                log.info(
                        "KMDb 포스터 제목 매칭 - 개봉일 차이 - 제목: {}, 개봉일: {}",
                        title,
                        releaseDate
                );

                return fallbackPosterUrl;
            }

            return null;

        } catch (Exception e) {

            log.warn(
                    "KMDb 포스터 조회 실패 - 제목: {}, 개봉일: {}",
                    title,
                    releaseDate,
                    e
            );

            return null;
        }
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
