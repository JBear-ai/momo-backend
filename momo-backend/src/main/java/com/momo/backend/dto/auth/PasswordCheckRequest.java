
package com.momo.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record PasswordCheckRequest(

        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword

) {}
