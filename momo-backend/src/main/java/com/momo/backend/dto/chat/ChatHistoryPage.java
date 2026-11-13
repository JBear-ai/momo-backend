package com.momo.backend.dto.chat;

import java.util.List;

/** 최근 메시지 일부와 이전 기록 조회를 위한 커서를 반환한다. */
public record ChatHistoryPage(
        List<ChatMessageResponse> messages,
        boolean hasMore,
        Long nextBeforeId,
        Long nextAfterId) {

    public ChatHistoryPage(List<ChatMessageResponse> messages, boolean hasMore, Long nextBeforeId) {
        this(messages, hasMore, nextBeforeId, null);
    }
}
