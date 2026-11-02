
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
    private final ObjectMapper objectMapper;

    // =========================================================
    // 영화 제목과 개봉일로 공통 포스터 검색
    // =========================================================

    public String findPosterUrl(String title, String releaseDate) {

        if (title == null || title.isBlank()) {
            return null;
        }

        try {

            String normalizedTitle = normalizeTitle(title);
            String normalizedDate = normalizeDate(releaseDate);

            // 1. KMDb API 검색
            String json = kmdbApiClient.searchMovie(title);

            if (json == null || json.isBlank()) {
                return null;
            }

            // 2. JSON 응답 파싱
            JsonNode dataList = objectMapper.readTree(json)
                    .path("Data");

            if (!dataList.isArray() || dataList.isEmpty()) {
                return null;
            }

            String fallbackPosterUrl = null;

            // 3. 모든 Data 검색
            for (JsonNode data : dataList) {

                JsonNode results = data.path("Result");

                if (!results.isArray() || results.isEmpty()) {
                    continue;
                }

                // 4. 모든 영화 검색
                for (JsonNode result : results) {

                    String kmdbTitle = normalizeTitle(
                            getText(result, "title")
                    );

                    String kmdbReleaseDate = normalizeDate(
                            getText(result, "repRlsDate")
                    );

                    // 5. 영화 제목 비교
                    if (!normalizedTitle.equalsIgnoreCase(kmdbTitle)) {
                        continue;
                    }

                    // 6. 포스터 주소 추출
                    String posterUrl = extractPoster(result);

                    if (posterUrl == null) {
                        continue;
                    }

                    // 7. 개봉일이 정확하게 일치하는 결과 우선 반환
                    if (!normalizedDate.isBlank()
                            && normalizedDate.equals(kmdbReleaseDate)) {

                        log.info(
                                "KMDb 포스터 검색 성공 - 제목: {}, 개봉일: {}",
                                title,
                                releaseDate
                        );

                        return posterUrl;
                    }

                    // 8. 개봉일 정보가 없는 경우에만 보조 후보로 저장
                    if (fallbackPosterUrl == null
                            && (normalizedDate.isBlank()
                            || kmdbReleaseDate.isBlank())) {

                        fallbackPosterUrl = posterUrl;
                    }
                }
            }

            // 9. 정확한 개봉일 일치 결과가 없으면 보조 후보 반환
            if (fallbackPosterUrl != null) {

                log.info(
                        "KMDb 포스터 보조 매칭 성공 - 제목: {}, 개봉일: {}",
                        title,
                        releaseDate
                );

                return fallbackPosterUrl;
            }

            log.info(
                    "KMDb 포스터 검색 결과 없음 - 제목: {}, 개봉일: {}",
                    title,
                    releaseDate
            );

            return null;

        } catch (Exception e) {

            log.warn(
                    "KMDb 포스터 조회 실패 - 영화 제목: {}, 개봉일: {}",
                    title,
                    releaseDate,
                    e
            );

            return null;
        }
    }

    // =========================================================
    // 포스터 URL 추출
    // =========================================================

    private String extractPoster(JsonNode result) {

        String posters = getText(result, "posters");

        if (posters.isBlank()) {
            return null;
        }

        String[] posterUrls = posters.split("\\|");

        for (String url : posterUrls) {

            String trimmedUrl = url.trim();

            if (!trimmedUrl.isBlank()) {
                return trimmedUrl;
            }
        }

        return null;
    }

    // =========================================================
    // JSON 문자열 안전하게 가져오기
    // =========================================================

    private String getText(JsonNode node, String fieldName) {

        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return "";
        }

        return value.asText("").trim();
    }

    // =========================================================
    // 개봉일 형식 정리
    // =========================================================

    private String normalizeDate(String date) {

        if (date == null || date.isBlank()) {
            return "";
        }

        return date.replaceAll("[^0-9]", "");
    }

    // =========================================================
    // 영화 제목 비교용 문자열 정리
    // =========================================================

    private String normalizeTitle(String title) {

        if (title == null) {
            return "";
        }

        return title
                .replace("!HS", "")
                .replace("!HE", "")
                .replaceAll("\\s+", "")
                .trim();
    }
}
