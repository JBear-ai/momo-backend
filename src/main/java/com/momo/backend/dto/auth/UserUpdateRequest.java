package com.momo.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        // 변경할 닉네임
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(max = 12, message = "닉네임은 12자 이하여야 합니다.")
        String nickname,

        // 변경할 이메일
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {}