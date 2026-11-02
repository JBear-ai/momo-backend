package com.momo.backend.dto.movie;

import java.util.List;

// 영화 검색 페이지 응답 DTO
public record MovieSearchPageResponse(
        List<MovieSearchResponse> content, // 영화 검색 결과
        int page,                           // 현재 페이지
        int size,                           // 페이지당 영화 수
        int totalCount,                     // 전체 검색 결과 수
        boolean hasNext                     // 다음 페이지 존재 여부
) {
}