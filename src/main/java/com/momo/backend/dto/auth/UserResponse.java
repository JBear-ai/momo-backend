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

    // User Entity에서 응답에 필요한 정보만 가져옴
    public UserResponse(User user) {
        this.userId = user.getUserId();
        this.loginId = user.getLoginId();
        this.nickname = user.getNickname();
    }
}