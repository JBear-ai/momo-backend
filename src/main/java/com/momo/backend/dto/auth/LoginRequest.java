package com.momo.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    // 로그인할 때 입력하는 아이디
    @NotBlank
    private String loginId;

    // 로그인할 때 입력하는 비밀번호
    @NotBlank
    private String password;
}