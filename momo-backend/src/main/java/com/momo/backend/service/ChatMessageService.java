package com.momo.backend.service;

import com.momo.backend.dto.chat.ChatHistoryPage;
import com.momo.backend.dto.chat.ChatMessageResponse;
import com.momo.backend.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** 공용 채팅의 메모리 보관, 입력 검증, 속도 제한, 본인 삭제를 담당한다. */
@Service
public class ChatMessageService {

    private static final int MAX_STORED_MESSAGES = 1_000;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_MESSAGE_CODE_POINTS = 500;
    private static final long RATE_WINDOW_NANOS = 3_000_000_000L;
    private static final int MAX_MESSAGES_PER_WINDOW = 3;

    private final ChatProfanityFilter profanityFilter;
    private final ChatImageStorageService imageStorageService;
    private final Deque<ChatMessageResponse> messages = new ArrayDeque<>();
    private final Map<Long, Deque<Long>> memberSendTimes = new HashMap<>();
    private final AtomicLong nextMessageId = new AtomicLong(1);

    @Autowired
    public ChatMessageService(ChatProfanityFilter profanityFilter, ChatImageStorageService imageStorageService) {
        this.profanityFilter = profanityFilter;
        this.imageStorageService = imageStorageService;
    }

    public ChatMessageService(ChatProfanityFilter profanityFilter) {
        this(profanityFilter, null);
    }

    // 연결이 여러 개에서 동시에 요청돼도 메시지 큐와 회원별 전송 시각을 한 번에 갱신한다.
    /** 검증된 회원 정보로만 작성자를 구성하고, 허용된 메시지만 보관·반환한다. */
    public synchronized ChatMessageResponse send(User user, String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new ChatRuleViolationException("EMPTY_MESSAGE", "메시지를 입력해 주세요.");
        }

        String content = rawContent.strip();
        if (content.codePointCount(0, content.length()) > MAX_MESSAGE_CODE_POINTS) {
            throw new ChatRuleViolationException("MESSAGE_TOO_LONG", "메시지는 500자 이하로 입력해 주세요.");
        }
        if (profanityFilter.containsBannedWord(content)) {
            throw new ChatRuleViolationException("BANNED_WORD", "금칙어가 포함된 메시지는 보낼 수 없습니다.");
        }

        enforceRateLimit(user.getUserId());

        ChatMessageResponse message = new ChatMessageResponse(
                nextMessageId.getAndIncrement(),
                user.getUserId(),
                user.getNickname(),
                user.getProfileImage() != null && !user.getProfileImage().isBlank(),
                content,
                Instant.now().toString());

        messages.addLast(message);
        removeOverflowImage();
        return message;
    }

    // 업로더를 확인하고 일반 메시지와 같은 제한 및 보관 정책을 적용한다.
    public synchronized ChatMessageResponse sendImage(User user, String imageId) {
        if (imageId == null || imageStorageService == null) {
            throw new ChatRuleViolationException("IMAGE_UNAVAILABLE", "이미지를 사용할 수 없습니다.");
        }
        enforceRateLimit(user.getUserId());
        imageStorageService.attachToMessage(imageId, user.getUserId());
        ChatMessageResponse message = new ChatMessageResponse(
                nextMessageId.getAndIncrement(), user.getUserId(), user.getNickname(),
                user.getProfileImage() != null && !user.getProfileImage().isBlank(),
                "", Instant.now().toString(), "IMAGE", imageId);
        messages.addLast(message);
        removeOverflowImage();
        return message;
    }

    /** 첫 화면은 최근 50개, 추가 조회는 전달된 ID보다 오래된 페이지를 반환한다. */
    public synchronized ChatHistoryPage getHistory(Long beforeId, Integer requestedSize) {
        int pageSize = requestedSize == null ? DEFAULT_PAGE_SIZE : requestedSize;
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        List<ChatMessageResponse> eligible = messages.stream()
                .filter(message -> beforeId == null || message.id() < beforeId)
                .toList();
        int firstIndex = Math.max(0, eligible.size() - pageSize);
        List<ChatMessageResponse> page = new ArrayList<>(eligible.subList(firstIndex, eligible.size()));
        boolean hasMore = firstIndex > 0;
        Long nextBeforeId = page.isEmpty() ? null : page.getFirst().id();
        return new ChatHistoryPage(List.copyOf(page), hasMore, nextBeforeId, null);
    }

    /** 재연결 시 마지막 수신 ID 이후의 보관 메시지를 작은 페이지로 이어서 반환한다. */
    public synchronized ChatHistoryPage getMessagesAfter(Long afterId, Integer requestedSize) {
        int pageSize = requestedSize == null || requestedSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(requestedSize, MAX_PAGE_SIZE);
        long cursor = afterId == null ? 0 : afterId;
        List<ChatMessageResponse> eligible = messages.stream()
                .filter(message -> message.id() > cursor)
                .toList();
        int endIndex = Math.min(pageSize, eligible.size());
        List<ChatMessageResponse> page = new ArrayList<>(eligible.subList(0, endIndex));
        boolean hasMore = eligible.size() > endIndex;
        Long nextAfterId = page.isEmpty() ? cursor : page.getLast().id();
        return new ChatHistoryPage(List.copyOf(page), hasMore, null, nextAfterId);
    }

    /** 보관 중인 메시지만 작성자 ID를 대조해 삭제하고, 다른 회원의 삭제는 거부한다. */
    public synchronized void deleteOwnMessage(User user, long messageId) {
        ChatMessageResponse target = messages.stream()
                .filter(message -> message.id() == messageId)
                .findFirst()
                .orElseThrow(() -> new ChatRuleViolationException("MESSAGE_NOT_FOUND", "메시지를 찾을 수 없습니다."));

        if (target.userId() != user.getUserId()) {
            throw new ChatRuleViolationException("NOT_MESSAGE_OWNER", "본인이 작성한 메시지만 삭제할 수 있습니다.");
        }
        messages.remove(target);
        if ("IMAGE".equals(target.messageType())) {
            imageStorageService.delete(target.imageId());
        }
    }

    // 현재 채팅 이력에서 이미지 식별자를 확인한다.
    public synchronized boolean containsImage(String imageId) {
        return messages.stream().anyMatch(message -> "IMAGE".equals(message.messageType())
                && imageId.equals(message.imageId()));
    }

    private void removeOverflowImage() {
        while (messages.size() > MAX_STORED_MESSAGES) {
            ChatMessageResponse removed = messages.removeFirst();
            if ("IMAGE".equals(removed.messageType())) {
                imageStorageService.delete(removed.imageId());
            }
        }
    }

    private void enforceRateLimit(Long userId) {
        long now = System.nanoTime();
        // 최근 3초 기록이 끝난 회원은 제거해 장시간 서버에서 제한 상태가 누적되지 않게 한다.
        Iterator<Map.Entry<Long, Deque<Long>>> iterator = memberSendTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Deque<Long> times = iterator.next().getValue();
            while (!times.isEmpty() && now - times.peekFirst() >= RATE_WINDOW_NANOS) {
                times.removeFirst();
            }
            if (times.isEmpty()) {
                iterator.remove();
            }
        }

        Deque<Long> sendTimes = memberSendTimes.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        if (sendTimes.size() >= MAX_MESSAGES_PER_WINDOW) {
            throw new ChatRuleViolationException("RATE_LIMITED", "최근 3초 동안 최대 3개 메시지만 보낼 수 있습니다.");
        }
        sendTimes.addLast(now);
    }
}
