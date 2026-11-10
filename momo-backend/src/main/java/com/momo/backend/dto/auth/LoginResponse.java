package com.momo.backend.dto.auth;

import com.momo.backend.entity.User;

public record LoginResponse(
        Long userId,
        String loginId,
        String nickname,
        String profileImage,
        String accessToken,
        String refreshToken,
        String tokenType
) {

    public LoginResponse(User user, String accessToken, String refreshToken) {
        this(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname(),
                user.getProfileImage(),
                accessToken,
                refreshToken,
                "Bearer"
        );
    }
}
