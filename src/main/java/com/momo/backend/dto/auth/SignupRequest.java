package com.momo.backend.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Pattern;


@Getter
@NoArgsConstructor
public class SignupRequest {

    // 회원이 가입할 때 사용할 로그인 아이디
    @NotBlank
    @Size(min = 4, max = 20)
    private String loginId;

    // 회원가입 시 입력한 비밀번호 (암호화 전)
    @NotBlank
    @Size(min = 8)
    private String password;

    // 화면에서 사용할 닉네임
    @NotBlank
    @Size(min = 2, max = 12)
    private String nickname;

    // 선택 입력 이메일 (입력했다면 이메일 형식 검사)
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$",
            message = "올바른 이메일 형식이 아닙니다."
    )
    private String email;
}