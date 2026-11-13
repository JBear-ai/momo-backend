package com.momo.backend.dto.chat;

import java.util.List;

/** 클라이언트에 전달하는 채팅 이벤트의 공통 형식이다. */
public record ChatServerMessage(
        String type,
        ChatMessageResponse message,
        List<ChatMessageResponse> messages,
        Boolean hasMore,
        Long nextBeforeId,
        Long userId,
        Long messageId,
        String code,
        String detail,
        Long nextAfterId,
        Integer onlineCount,
        String streamId) {

    public static ChatServerMessage authenticated(long userId, String streamId) {
        return new ChatServerMessage("AUTHENTICATED", null, null, null, null,
                userId, null, null, null, null, null, streamId);
    }

    public static ChatServerMessage history(ChatHistoryPage page) {
        return new ChatServerMessage("HISTORY", null, page.messages(), page.hasMore(),
                page.nextBeforeId(), null, null, null, null, page.nextAfterId(), null, null);
    }

    public static ChatServerMessage sync(ChatHistoryPage page) {
        return new ChatServerMessage("SYNC", null, page.messages(), page.hasMore(),
                null, null, null, null, null, page.nextAfterId(), null, null);
    }

    public static ChatServerMessage newMessage(ChatMessageResponse message) {
        return new ChatServerMessage("MESSAGE", message, null, null, null,
                null, null, null, null, null, null, null);
    }

    public static ChatServerMessage deleted(long messageId) {
        return new ChatServerMessage("MESSAGE_DELETED", null, null, null, null,
                null, messageId, null, "작성자가 메시지를 삭제했습니다.", null, null, null);
    }

    public static ChatServerMessage presence(int onlineCount) {
        return new ChatServerMessage("PRESENCE", null, null, null, null,
                null, null, null, null, null, onlineCount, null);
    }

    public static ChatServerMessage error(String code, String detail) {
        return new ChatServerMessage("ERROR", null, null, null, null,
                null, null, code, detail, null, null, null);
    }
}
