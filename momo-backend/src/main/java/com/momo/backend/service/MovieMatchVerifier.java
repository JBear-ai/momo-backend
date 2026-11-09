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

        // 한글 제목 또는 영문 제목이 일치하는지 확인
        boolean titleMatches = matches(requestedTitle, foundTitle, foundOriginalTitle)
                || (!requestedTitleEn.isBlank()
                && matches(requestedTitleEn, foundTitle, foundOriginalTitle));

        if (!titleMatches) {
            return false;
        }

        String kobisYear = releaseYear(kobisReleaseDate);
        String candidateYear = releaseYear(candidateReleaseDate);

        // KOBIS 개봉 연도가 있으면 후보도 같은 연도여야 한다.
        if (!kobisYear.isBlank()) {
            return !candidateYear.isBlank() && kobisYear.equals(candidateYear);
        }

        // 개봉일이 없을 때 제목 하나만으로 승인하지 않는다. 영문·원제 또는
        // 감독이 함께 일치할 때만 동명 영화 오매칭 위험을 감수할 수 있다.
        boolean originalTitleMatches = !requestedTitleEn.isBlank()
                && matches(requestedTitleEn, foundTitle, foundOriginalTitle);
        boolean directorMatches = matchesDirector(kobisDirector, candidateDirector);
        return originalTitleMatches || directorMatches;
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

    // 비교 대상 중 일치하는 문자열이 있는지 확인
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

    // 제목의 공백·특수문자 등을 제거하여 비교 형식 통일
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

    // 개봉일 문자열에서 연도 4자리 추출
    private static String releaseYear(String date) {
        if (date == null) {
            return "";
        }
        String digits = date.replaceAll("[^0-9]", "");
        return digits.length() >= 4 ? digits.substring(0, 4) : "";
    }
}
