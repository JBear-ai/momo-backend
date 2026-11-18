package com.momo.backend.dto.auth;

import com.momo.backend.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {

    // 회원 고유 번호
    private final Long userId;

    // 회원 로그인 아이디
    private final String loginId;

    // 화면에 표시할 닉네임
    private final String nickname;

    // 화면에 표시할 이메일
    private final String email;

    // Firebase Storage에 저장된 프로필 이미지 URL
    private final String profileImage;

    // User Entity에서 응답에 필요한 정보만 가져옴
    public UserResponse(User user) {
        this.userId = user.getUserId();
        this.loginId = user.getLoginId();
        this.nickname = user.getNickname();
        this.email = user.getEmail();

        // 저장된 프로필 이미지 URL을 프론트에 전달
        this.profileImage = user.getProfileImage();
    }
}