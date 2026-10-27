package com.momo.backend.exception;

// 오류 코드와 메시지를 전달하는 사용자 정의 예외
public class CustomException extends RuntimeException {

    private final String code;

    public CustomException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}