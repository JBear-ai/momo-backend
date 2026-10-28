package com.momo.backend.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewCreateRequest {

    // KOBIS 영화 식별 코드
    @NotBlank(message = "영화 코드는 필수입니다.")
    @Size(max = 20, message = "영화 코드는 20자 이하여야 합니다.")
    private String movieCode;

    // 리뷰 제목
    @NotBlank(message = "리뷰 제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    // 리뷰 본문
    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 2000, message = "리뷰 내용은 2000자 이하여야 합니다.")
    private String content;

    // 영화 평가 (true: 좋아요, false: 싫어요)
    @NotNull(message = "영화 평가는 필수입니다.")
    private Boolean liked;
}