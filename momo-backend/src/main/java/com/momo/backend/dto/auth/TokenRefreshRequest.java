package com.momo.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank(message = "Refresh Token을 입력해 주세요.")
        String refreshToken
) {
}
