package com.momo.backend.dto.auth;

import com.momo.backend.entity.User;

public record LoginResponse(
        Long userId,
        String loginId,
        String nickname,
        String profileImage,
        String accessToken,
        String tokenType
) {

    public LoginResponse(User user, String accessToken) {
        this(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname(),
                user.getProfileImage(),
                accessToken,
                "Bearer"
        );
    }
}
