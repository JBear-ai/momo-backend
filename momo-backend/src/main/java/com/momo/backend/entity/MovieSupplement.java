package com.momo.backend.service;

import java.util.List;

/**
 * KMDb·TMDb에서 동일 영화로 검증된 보완 정보를 저장하는 DTO.
 * KOBIS에 누락된 항목을 채울 때 사용한다.
 */

public record MovieSupplement(
        String titleEn,
        String releaseDate,
        String showTime,
        String genre,
        String director,
        List<String> actors,
        String watchGrade,
        String source
) {
}
