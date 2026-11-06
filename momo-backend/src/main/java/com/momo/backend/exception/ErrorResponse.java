package com.momo.backend.exception;

// API 오류 응답 DTO
public record ErrorResponse(
        String code,       // 오류 코드
        String message     // 오류 메시지
) {
}