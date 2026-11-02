
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
     * 2. TMDb 검색
     * 3. 모두 실패하면 null 반환
     */
    public String findPosterUrl(
            String movieCode,
            String title,
            String releaseDate) {

        if (title == null || title.isBlank()) {
            log.warn("포스터 검색 실패 - 영화 제목이 비어 있습니다.");
            return null;
        }

        log.info(
                "포스터 검색 요청 - 영화 코드: {}, 제목: {}, 개봉일: {}",
                movieCode, title, releaseDate
        );

        // 1. KMDb 검색
        String kmdbPosterUrl = findKmdbPosterUrl(title, releaseDate);

        if (kmdbPosterUrl != null) {
            log.info("KMDb 포스터 검색 성공 - 영화 코드: {}", movieCode);
            return kmdbPosterUrl;
        }

        log.warn(
                "KMDb 포스터 검색 실패 - TMDb로 전환 - 영화 코드: {}, 제목: {}",
                movieCode, title
        );

        // 2. TMDb 검색
        String tmdbPosterUrl = tmdbService.findMoviePosterUrl(
                title,
                releaseDate
        );

        if (tmdbPosterUrl != null) {
            log.info(
                    "TMDb 포스터 사용 - 영화 코드: {}, 제목: {}",
                    movieCode, title
            );
            return tmdbPosterUrl;
        }

        // 3. 포스터 검색 실패
        log.warn(
                "포스터 검색 최종 실패 - 영화 코드: {}, 제목: {}, 개봉일: {}",
                movieCode, title, releaseDate
        );

        return null;
    }

    /**
     * 기존 메서드 호환용
     */
    public String findPosterUrl(String title, String releaseDate) {

        String kmdbPosterUrl = findKmdbPosterUrl(title, releaseDate);

        if (kmdbPosterUrl != null) {
            return kmdbPosterUrl;
        }

        return tmdbService.findMoviePosterUrl(title, releaseDate);
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

            log.info(
                    "KMDb API 요청 - 원본 제목: {}, 정규화 제목: {}, 개봉일: {}",
                    title, normalizedTitle, normalizedDate
            );

            String json = kmdbApiClient.searchMovie(title);

            if (json == null || json.isBlank()) {
                log.warn("KMDb API 응답이 비어 있습니다. 제목: {}", title);
                return null;
            }

            JsonNode dataList = objectMapper.readTree(json).path("Data");

            if (!dataList.isArray() || dataList.isEmpty()) {
                log.warn(
                        "KMDb Data 검색 결과 없음 - 제목: {}, Data: {}",
                        title, dataList
                );
                return null;
            }

            log.info(
                    "KMDb Data 결과 개수: {}",
                    dataList.size()
            );

            String fallbackPosterUrl = null;

            for (JsonNode data : dataList) {

                JsonNode results = data.path("Result");

                if (!results.isArray() || results.isEmpty()) {
                    log.warn("KMDb Result 배열이 비어 있습니다.");
                    continue;
                }

                log.info(
                        "KMDb 영화 후보 개수: {}",
                        results.size()
                );

                for (JsonNode result : results) {

                    String rawTitle = getText(result, "title");
                    String rawDate = getText(result, "repRlsDate");
                    String rawPosters = getText(result, "posters");

                    String kmdbTitle = normalizeTitle(rawTitle);
                    String kmdbReleaseDate = normalizeDate(rawDate);

                    log.info(
                            "KMDb 후보 - 제목: [{}], 정규화 제목: [{}], " +
                                    "개봉일: [{}], 정규화 개봉일: [{}], 포스터 존재: {}",
                            rawTitle,
                            kmdbTitle,
                            rawDate,
                            kmdbReleaseDate,
                            !rawPosters.isBlank()
                    );

                    // 제목 일치 여부 확인
                    if (!normalizedTitle.equals(kmdbTitle)) {
                        log.info(
                                "KMDb 제목 불일치 - 요청: [{}], 검색: [{}]",
                                normalizedTitle, kmdbTitle
                        );
                        continue;
                    }

                    String posterUrl = extractPoster(result);

                    if (posterUrl == null) {
                        log.info(
                                "KMDb 제목 일치했으나 포스터 URL 없음 - 제목: {}",
                                rawTitle
                        );
                        continue;
                    }

                    // 제목과 개봉일 모두 일치
                    if (!normalizedDate.isBlank()
                            && normalizedDate.equals(kmdbReleaseDate)) {

                        log.info(
                                "KMDb 포스터 정확 매칭 성공 - 제목: {}, 개봉일: {}",
                                rawTitle, rawDate
                        );

                        return posterUrl;
                    }

                    // 개봉일이 달라도 제목이 일치하면 후보로 저장
                    if (fallbackPosterUrl == null) {
                        fallbackPosterUrl = posterUrl;
                    }
                }
            }

            if (fallbackPosterUrl != null) {
                log.info(
                        "KMDb 제목 매칭 성공 - 개봉일 차이 - 제목: {}",
                        title
                );
                return fallbackPosterUrl;
            }

            log.warn(
                    "KMDb 최종 검색 결과 없음 - 제목: {}, 개봉일: {}",
                    title, releaseDate
            );

            return null;

        } catch (Exception e) {

            log.error(
                    "KMDb 포스터 조회 중 예외 발생 - 제목: {}, 개봉일: {}",
                    title, releaseDate, e
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
