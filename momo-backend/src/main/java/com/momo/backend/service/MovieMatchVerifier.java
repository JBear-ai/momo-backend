package com.momo.backend.service;

import java.util.Locale;

// KOBIS와 외부 API의 영화 정보를 비교하여 동일 작품인지 검증
final class MovieMatchVerifier {

    private MovieMatchVerifier() {
    }

    // 제목을 우선 비교하고 개봉 연도 또는 추가 정보로 동일 영화 판단
    static boolean isSameMovie(
            String kobisTitle,
            String kobisTitleEn,
            String kobisReleaseDate,
            String kobisDirector,
            String candidateTitle,
            String candidateOriginalTitle,
            String candidateReleaseDate,
            String candidateDirector) {

        String requestedTitle = normalizeTitle(kobisTitle);
        String requestedTitleEn = normalizeTitle(kobisTitleEn);
        String foundTitle = normalizeTitle(candidateTitle);
        String foundOriginalTitle = normalizeTitle(candidateOriginalTitle);

        boolean titleMatches = matches(requestedTitle, foundTitle, foundOriginalTitle)
                || (!requestedTitleEn.isBlank()
                && matches(requestedTitleEn, foundTitle, foundOriginalTitle));

        if (!titleMatches) {
            return false;
        }

        String kobisYear = releaseYear(kobisReleaseDate);
        String candidateYear = releaseYear(candidateReleaseDate);

        // 양쪽 개봉연도가 확인되면 반드시 같은 연도여야 한다.
        if (!kobisYear.isBlank() && !candidateYear.isBlank()) {
            if (!kobisYear.equals(candidateYear)) {
                return false;
            }

            // 두 공급원의 감독명이 같은 문자 체계로 제공되었는데 다르면 동명 영화 오매칭을 막는다.
            boolean directorsProvided = kobisDirector != null && !kobisDirector.isBlank()
                    && candidateDirector != null && !candidateDirector.isBlank();
            if (directorsProvided
                    && sameNameScript(kobisDirector, candidateDirector)
                    && !matchesDirector(kobisDirector, candidateDirector)) {
                return false;
            }

            return true;
        }

        // 개봉연도가 없으면 영문 원제 또는 감독까지 일치해야 한다.
        boolean originalTitleMatches = !requestedTitleEn.isBlank()
                && matches(requestedTitleEn, foundTitle, foundOriginalTitle);
        boolean directorMatches = matchesDirector(kobisDirector, candidateDirector);
        return originalTitleMatches || directorMatches;
    }

    // 동일 영화 후보 중 제목·개봉일·감독 일치도가 높은 포스터를 우선한다.
    static int matchScore(
            String kobisTitle,
            String kobisTitleEn,
            String kobisReleaseDate,
            String kobisDirector,
            String candidateTitle,
            String candidateOriginalTitle,
            String candidateReleaseDate,
            String candidateDirector) {

        if (!isSameMovie(
                kobisTitle, kobisTitleEn, kobisReleaseDate, kobisDirector,
                candidateTitle, candidateOriginalTitle,
                candidateReleaseDate, candidateDirector)) {
            return -1;
        }

        String requestedTitle = normalizeTitle(kobisTitle);
        String requestedTitleEn = normalizeTitle(kobisTitleEn);
        String foundTitle = normalizeTitle(candidateTitle);
        String foundOriginalTitle = normalizeTitle(candidateOriginalTitle);

        int score = 0;

        if (matches(requestedTitle, foundTitle)) {
            score += 100;
        } else if (matches(requestedTitle, foundOriginalTitle)) {
            score += 95;
        } else if (matches(requestedTitleEn, foundTitle)) {
            score += 90;
        } else if (matches(requestedTitleEn, foundOriginalTitle)) {
            score += 85;
        }

        String requestedDate = normalizeDate(kobisReleaseDate);
        String foundDate = normalizeDate(candidateReleaseDate);

        if (!requestedDate.isBlank() && !foundDate.isBlank()) {
            if (requestedDate.equals(foundDate)) {
                score += 30;
            } else if (releaseYear(requestedDate).equals(releaseYear(foundDate))) {
                score += 20;
            }
        }

        if (matchesDirector(kobisDirector, candidateDirector)) {
            score += 25;
        }

        return score;
    }

    private static boolean sameNameScript(String first, String second) {
        return containsHangul(first) == containsHangul(second);
    }

    private static boolean containsHangul(String value) {
        return value != null && value.codePoints()
                .anyMatch(codePoint -> codePoint >= 0xAC00 && codePoint <= 0xD7A3);
    }

    // 외부 API가 여러 감독을 반환해도 KOBIS 감독 한 명과 비교할 수 있도록 처리
    private static boolean matchesDirector(String requestedDirector, String candidateDirector) {
        String requested = normalizeTitle(requestedDirector);
        if (requested.isBlank() || candidateDirector == null || candidateDirector.isBlank()) {
            return false;
        }

        if (requested.equals(normalizeTitle(candidateDirector))) {
            return true;
        }

        for (String name : candidateDirector.split("[,|]")) {
            if (requested.equals(normalizeTitle(name))) {
                return true;
            }
        }

        return false;
    }

    private static boolean matches(String requested, String... candidates) {
        if (requested.isBlank()) {
            return false;
        }
        for (String candidate : candidates) {
            if (requested.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    static String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title
                .replace("!HS", "")
                .replace("!HE", "")
                .replaceAll("\\s+", "")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String normalizeDate(String date) {
        return date == null ? "" : date.replaceAll("[^0-9]", "");
    }

    private static String releaseYear(String date) {
        String digits = normalizeDate(date);
        return digits.length() >= 4 ? digits.substring(0, 4) : "";
    }
}