package com.momo.backend.dto.chat;

/** 브라우저가 보내는 채팅 프로토콜 프레임이며, 작성자 정보는 포함하지 않는다. */
public record ChatClientMessage(
        String type,
        String accessToken,
        String content,
        Long beforeId,
        Integer limit,
        Long messageId,
        Long afterId,
        String imageId) {

    public ChatClientMessage(String type, String accessToken, String content,
                             Long beforeId, Integer limit, Long messageId, Long afterId) {
        this(type, accessToken, content, beforeId, limit, messageId, afterId, null);
    }
}
