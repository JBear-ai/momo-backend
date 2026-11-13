package com.momo.backend.websocket;

import com.momo.backend.dto.chat.ChatClientMessage;
import com.momo.backend.dto.chat.ChatHistoryPage;
import com.momo.backend.dto.chat.ChatServerMessage;
import com.momo.backend.entity.User;
import com.momo.backend.repository.UserRepository;
import com.momo.backend.security.TokenProvider;
import com.momo.backend.service.ChatMessageService;
import com.momo.backend.service.ChatRuleViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/** 인증된 연결만 공용 채팅 명령을 처리하고 메시지를 전체 방에 전달한다. */
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String AUTHENTICATED_ATTRIBUTE = "momoChatAuthenticated";
    private static final String LOGIN_ID_ATTRIBUTE = "momoChatLoginId";
    private static final String MEMBER_ID_ATTRIBUTE = "momoChatMemberId";
    private static final String ACCESS_TOKEN_ATTRIBUTE = "momoChatAccessToken";
    private static final String AUTH_TIMEOUT_ATTRIBUTE = "momoChatAuthTimeout";
    private static final String TOKEN_TIMEOUT_ATTRIBUTE = "momoChatTokenTimeout";
    private static final long AUTH_TIMEOUT_SECONDS = 10;
    private static final CloseStatus AUTH_FAILURE = CloseStatus.POLICY_VIOLATION;

    private final ObjectMapper objectMapper;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final ChatMessageService chatMessageService;
    private final ScheduledExecutorService chatWebSocketScheduler;
    private final Set<WebSocketSession> authenticatedSessions = ConcurrentHashMap.newKeySet();
    private final Object presenceLock = new Object();
    private final Map<Long, Set<WebSocketSession>> sessionsByMember = new HashMap<>();
    private final String streamId = UUID.randomUUID().toString();
    private boolean publishingPresence;
    private boolean presenceChangedDuringBroadcast;

    /** 인증 프레임이 오지 않는 연결을 오래 점유하지 않도록 제한 시간을 건다. */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 인증 프레임을 보내지 않고 연결만 점유하는 경우도 10초 안에 종료한다.
        ScheduledFuture<?> timeout = chatWebSocketScheduler.schedule(
                () -> closeSession(session, AUTH_FAILURE), AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        session.getAttributes().put(AUTH_TIMEOUT_ATTRIBUTE, timeout);
    }

    /** 프레임 유형에 따라 인증을 시작하거나, 토큰이 아직 유효한 요청만 처리한다. */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws IOException {
        ChatClientMessage request;
        try {
            request = objectMapper.readValue(textMessage.getPayload(), ChatClientMessage.class);
        } catch (JacksonException exception) {
            // 파싱 세부정보를 노출하지 않고 잘못된 프레임만 거부하며, 미인증 연결은 닫는다.
            sendError(session, "INVALID_FRAME", "채팅 요청 형식이 올바르지 않습니다.");
            if (!isAuthenticated(session)) {
                closeSession(session, AUTH_FAILURE);
            }
            return;
        }

        if (!isAuthenticated(session)) {
            authenticate(session, request);
            return;
        }

        String accessToken = (String) session.getAttributes().get(ACCESS_TOKEN_ATTRIBUTE);
        if (!tokenProvider.validateToken(accessToken)) {
            sendError(session, "ACCESS_TOKEN_EXPIRED", "로그인이 만료되었습니다. 채팅을 다시 열어 주세요.");
            closeSession(session, AUTH_FAILURE);
            return;
        }

        String loginId = (String) session.getAttributes().get(LOGIN_ID_ATTRIBUTE);
        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) {
            sendError(session, "MEMBER_NOT_FOUND", "회원 정보를 확인할 수 없어 연결을 종료합니다.");
            closeSession(session, AUTH_FAILURE);
            return;
        }

        try {
            handleAuthenticatedRequest(session, request, user);
        } catch (ChatRuleViolationException exception) {
            sendError(session, exception.getCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(session, "INVALID_REQUEST", "요청을 처리할 수 없습니다.");
        }
    }

    /** JWT 검증 후 DB 회원 정보로 세션 작성자를 확정하고 초기 이력을 보낸다. */
    private void authenticate(WebSocketSession session, ChatClientMessage request) throws IOException {
        if (request == null || !"AUTH".equals(request.type())
                || request.accessToken() == null || request.accessToken().isBlank()) {
            sendError(session, "AUTH_REQUIRED", "채팅 이용을 위해 로그인이 필요합니다.");
            closeSession(session, AUTH_FAILURE);
            return;
        }

        // Refresh Token은 채팅 인증에 쓰지 않고 기존 Access Token의 서명·만료·typ을 재검증한다.
        if (!tokenProvider.validateToken(request.accessToken())) {
            sendError(session, "INVALID_ACCESS_TOKEN", "유효한 Access Token이 필요합니다.");
            closeSession(session, AUTH_FAILURE);
            return;
        }

        String loginId;
        try {
            loginId = tokenProvider.getLoginId(request.accessToken());
        } catch (RuntimeException exception) {
            sendError(session, "INVALID_ACCESS_TOKEN", "유효한 Access Token이 필요합니다.");
            closeSession(session, AUTH_FAILURE);
            return;
        }

        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) {
            sendError(session, "MEMBER_NOT_FOUND", "회원 정보를 확인할 수 없습니다.");
            closeSession(session, AUTH_FAILURE);
            return;
        }

        session.getAttributes().put(LOGIN_ID_ATTRIBUTE, loginId);
        session.getAttributes().put(MEMBER_ID_ATTRIBUTE, user.getUserId());
        session.getAttributes().put(ACCESS_TOKEN_ATTRIBUTE, request.accessToken());
        cancelTimeout(session, AUTH_TIMEOUT_ATTRIBUTE);
        if (!scheduleTokenExpiry(session, request.accessToken())) {
            sendError(session, "ACCESS_TOKEN_EXPIRED", "로그인이 만료되었습니다. 다시 로그인해 주세요.");
            closeSession(session, AUTH_FAILURE);
            return;
        }

        // 서버 재시작으로 메모리 기록이 초기화됐는지 프론트가 구분할 수 있도록 streamId를 보낸다.
        sendFrame(session, ChatServerMessage.authenticated(user.getUserId(), streamId));
        sendFrame(session, ChatServerMessage.history(chatMessageService.getHistory(null, null)));
        if (session.isOpen()) {
            registerAuthenticatedSession(session, user.getUserId());
        }
    }

    /** 서버가 확인한 회원 ID를 기준으로 메시지 송신, 이력 조회, 본인 삭제를 수행한다. */
    private void handleAuthenticatedRequest(WebSocketSession session, ChatClientMessage request, User user)
            throws IOException {
        if (request == null || request.type() == null) {
            sendError(session, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다.");
            return;
        }

        switch (request.type()) {
            case "SEND" -> broadcast(ChatServerMessage.newMessage(
                    chatMessageService.send(user, request.content())));
            case "IMAGE" -> broadcast(ChatServerMessage.newMessage(
                    chatMessageService.sendImage(user, request.imageId())));
            case "HISTORY" -> sendFrame(session, ChatServerMessage.history(
                    chatMessageService.getHistory(request.beforeId(), request.limit())));
            case "SYNC" -> sendFrame(session, ChatServerMessage.sync(
                    chatMessageService.getMessagesAfter(request.afterId(), request.limit())));
            case "DELETE" -> {
                if (request.messageId() == null) {
                    sendError(session, "MESSAGE_ID_REQUIRED", "삭제할 메시지 정보가 필요합니다.");
                    return;
                }
                // 서버가 작성자 권한을 확인해 제거한 뒤 삭제 이벤트를 전체 연결에 동기화한다.
                chatMessageService.deleteOwnMessage(user, request.messageId());
                broadcast(ChatServerMessage.deleted(request.messageId()));
            }
            default -> sendError(session, "UNKNOWN_REQUEST", "지원하지 않는 채팅 요청입니다.");
        }
    }

    /** 만료된 수신자 연결은 제외해 채팅 데이터를 유효한 Access Token에만 전달한다. */
    private void broadcast(ChatServerMessage event) {
        for (WebSocketSession recipient : authenticatedSessions) {
            String accessToken = (String) recipient.getAttributes().get(ACCESS_TOKEN_ATTRIBUTE);
            if (!recipient.isOpen() || !tokenProvider.validateToken(accessToken)) {
                closeSession(recipient, AUTH_FAILURE);
                continue;
            }
            try {
                sendFrame(recipient, event);
            } catch (IOException exception) {
                closeSession(recipient, CloseStatus.SERVER_ERROR);
            }
        }
    }

    /** 회원별 연결 집합을 갱신해 여러 탭을 한 명으로 집계하고 최신 인원을 알린다. */
    private void registerAuthenticatedSession(WebSocketSession session, Long memberId) {
        synchronized (presenceLock) {
            session.getAttributes().put(AUTHENTICATED_ATTRIBUTE, true);
            sessionsByMember.computeIfAbsent(memberId, ignored -> new HashSet<>()).add(session);
            authenticatedSessions.add(session);
            broadcastPresenceLocked();
        }
    }

    /** 마지막 탭이 닫힐 때만 회원 인원을 줄이고, 중복 정리는 무시한다. */
    private void unregisterAuthenticatedSession(WebSocketSession session) {
        synchronized (presenceLock) {
            if (!authenticatedSessions.remove(session)) {
                return;
            }
            Object memberIdValue = session.getAttributes().get(MEMBER_ID_ATTRIBUTE);
            if (memberIdValue instanceof Long memberId) {
                Set<WebSocketSession> memberSessions = sessionsByMember.get(memberId);
                if (memberSessions != null) {
                    memberSessions.remove(session);
                    if (memberSessions.isEmpty()) {
                        sessionsByMember.remove(memberId);
                    }
                }
            }
            broadcastPresenceLocked();
        }
    }

    /** 인원 변경이 브로드캐스트 도중 발생해도 마지막 이벤트가 최신 인원을 담도록 반복 전송한다. */
    private void broadcastPresenceLocked() {
        if (publishingPresence) {
            presenceChangedDuringBroadcast = true;
            return;
        }

        publishingPresence = true;
        try {
            do {
                presenceChangedDuringBroadcast = false;
                ChatServerMessage event = ChatServerMessage.presence(sessionsByMember.size());
                List<WebSocketSession> recipients = new ArrayList<>(authenticatedSessions);
                for (WebSocketSession recipient : recipients) {
                    String accessToken = (String) recipient.getAttributes().get(ACCESS_TOKEN_ATTRIBUTE);
                    if (!recipient.isOpen() || !tokenProvider.validateToken(accessToken)) {
                        closeSession(recipient, AUTH_FAILURE);
                        continue;
                    }
                    try {
                        sendFrame(recipient, event);
                    } catch (IOException exception) {
                        closeSession(recipient, CloseStatus.SERVER_ERROR);
                    }
                }
            } while (presenceChangedDuringBroadcast);
        } finally {
            publishingPresence = false;
        }
    }

    private void sendError(WebSocketSession session, String code, String detail) throws IOException {
        sendFrame(session, ChatServerMessage.error(code, detail));
    }

    private void sendFrame(WebSocketSession session, ChatServerMessage message) throws IOException {
        String payload = objectMapper.writeValueAsString(message);
        // Spring WebSocket 세션은 동시 send를 허용하지 않아 연결 단위로 직렬화한다.
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
            }
        }
    }

    private boolean isAuthenticated(WebSocketSession session) {
        return Boolean.TRUE.equals(session.getAttributes().get(AUTHENTICATED_ATTRIBUTE));
    }

    /** 연결 종료 시 브로드캐스트 집합과 예약 작업을 함께 정리한다. */
    private void closeSession(WebSocketSession session, CloseStatus status) {
        unregisterAuthenticatedSession(session);
        cancelTimeout(session, AUTH_TIMEOUT_ATTRIBUTE);
        cancelTimeout(session, TOKEN_TIMEOUT_ATTRIBUTE);
        if (session.isOpen()) {
            try {
                session.close(status);
            } catch (IOException ignored) {
                // 연결이 이미 닫힌 경우 추가 작업은 하지 않는다.
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 정상 종료와 서버 종료 모두에서 남아 있는 세션/타이머 참조를 제거한다.
        unregisterAuthenticatedSession(session);
        cancelTimeout(session, AUTH_TIMEOUT_ATTRIBUTE);
        cancelTimeout(session, TOKEN_TIMEOUT_ATTRIBUTE);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        // 전송 계층 오류가 난 세션은 추가 메시지를 받지 않도록 즉시 종료한다.
        closeSession(session, CloseStatus.SERVER_ERROR);
    }

    /** 검증된 Access Token의 exp에 맞춰 소켓 종료를 예약한다. */
    private boolean scheduleTokenExpiry(WebSocketSession session, String accessToken) {
        if (!tokenProvider.validateToken(accessToken)) {
            return false;
        }
        long delayMillis = Duration.between(Instant.now(), tokenProvider.getExpiration(accessToken)).toMillis();
        if (delayMillis <= 0) {
            return false;
        }
        ScheduledFuture<?> timeout = chatWebSocketScheduler.schedule(
                () -> closeSession(session, AUTH_FAILURE), delayMillis, TimeUnit.MILLISECONDS);
        session.getAttributes().put(TOKEN_TIMEOUT_ATTRIBUTE, timeout);
        return true;
    }

    private void cancelTimeout(WebSocketSession session, String attribute) {
        Object timeout = session.getAttributes().remove(attribute);
        if (timeout instanceof ScheduledFuture<?> future) {
            future.cancel(false);
        }
    }
}
