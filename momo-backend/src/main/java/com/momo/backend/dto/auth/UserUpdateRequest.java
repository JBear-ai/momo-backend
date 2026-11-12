package com.momo.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        // 변경할 닉네임
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(min = 2, max = 12, message = "닉네임은 2~12자 입력해 주세요.")
        @jakarta.validation.constraints.Pattern(
                regexp = "^[가-힣a-zA-Z0-9]{2,12}$",
                message = "닉네임은 한글, 영문, 숫자로 2~12자 입력해 주세요."
        )
        String nickname,

        // 변경할 이메일
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        // 비밀번호 변경 시 입력하는 현재 비밀번호
        String currentPassword,

        // 변경할 새 비밀번호
        String newPassword

) {}