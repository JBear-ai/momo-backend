package com.momo.backend.dto.favorite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FavoriteMovieRequest(
        @NotBlank(message = "영화 코드가 필요합니다.")
        @Pattern(regexp = "^[A-Za-z0-9]{8}$", message = "올바른 영화 코드가 아닙니다.")
        String movieCode,

        @NotBlank(message = "영화 제목이 필요합니다.")
        @Size(max = 255, message = "영화 제목이 너무 깁니다.")
        String title,

        @Size(max = 20, message = "개봉일 정보가 너무 깁니다.")
        String releaseDate,

        @Size(max = 1000, message = "포스터 주소가 너무 깁니다.")
        String posterUrl
) {
}
