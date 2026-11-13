package com.momo.backend.dto.chat;

/** 서버가 작성자 정보를 검증한 뒤 브로드캐스트하는 메시지 표현이다. */
public record ChatMessageResponse(
        long id,
        long userId,
        String nickname,
        boolean profileImageAvailable,
        String content,
        String createdAt,
        String messageType,
        String imageId) {

    public ChatMessageResponse(long id, long userId, String nickname,
                               boolean profileImageAvailable, String content, String createdAt) {
        this(id, userId, nickname, profileImageAvailable, content, createdAt, "TEXT", null);
    }
}
