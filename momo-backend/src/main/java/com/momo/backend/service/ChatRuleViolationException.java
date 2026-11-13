package com.momo.backend.service;

/** 채팅 입력·속도·작성 권한 위반을 코드와 사용자 안내로 전달한다. */
public class ChatRuleViolationException extends RuntimeException {

    private final String code;

    public ChatRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
