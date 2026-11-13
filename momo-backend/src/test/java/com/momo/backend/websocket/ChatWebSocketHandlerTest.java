package com.momo.backend.websocket;

import com.momo.backend.dto.chat.ChatHistoryPage;
import com.momo.backend.dto.chat.ChatMessageResponse;
import com.momo.backend.entity.User;
import com.momo.backend.repository.UserRepository;
import com.momo.backend.security.TokenProvider;
import com.momo.backend.service.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatWebSocketHandlerTest {

    @Test
    void authenticatedImageCommandIsBroadcastToOtherAuthenticatedMembers() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        ChatWebSocketHandler handler = new ChatWebSocketHandler(
                objectMapper, tokenProvider, userRepository, messageService, scheduler());
        WebSocketSession session = session();
        WebSocketSession recipient = session();
        User user = mock(User.class);
        User recipientUser = mock(User.class);
        String imageId = "9c9bb1a8-f922-4ea1-8c41-2f2d987eadd1";

        when(tokenProvider.validateToken(any())).thenReturn(true);
        when(tokenProvider.getLoginId("valid-access-token")).thenReturn("member1");
        when(tokenProvider.getLoginId("recipient-token")).thenReturn("member2");
        when(tokenProvider.getExpiration(any())).thenReturn(Instant.now().plusSeconds(60));
        when(userRepository.findByLoginId("member1")).thenReturn(java.util.Optional.of(user));
        when(userRepository.findByLoginId("member2")).thenReturn(java.util.Optional.of(recipientUser));
        when(user.getUserId()).thenReturn(1L);
        when(recipientUser.getUserId()).thenReturn(2L);
        when(user.getNickname()).thenReturn("회원1");
        when(user.getProfileImage()).thenReturn(null);
        when(messageService.getHistory(null, null)).thenReturn(new ChatHistoryPage(List.of(), false, null));
        when(messageService.sendImage(user, imageId)).thenReturn(
                new ChatMessageResponse(1, 1, "회원1", false, "", Instant.now().toString(), "IMAGE", imageId));

        handler.handleTextMessage(session, authFrame("valid-access-token"));
        handler.handleTextMessage(recipient, authFrame("recipient-token"));
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"IMAGE\",\"imageId\":\"" + imageId + "\"}"));

        verify(messageService).sendImage(user, imageId);
        ArgumentCaptor<TextMessage> frames = ArgumentCaptor.forClass(TextMessage.class);
        verify(recipient, org.mockito.Mockito.atLeast(1)).sendMessage(frames.capture());
        assertTrue(frames.getAllValues().stream().anyMatch(frame -> {
            var json = objectMapper.readTree(frame.getPayload());
            return "MESSAGE".equals(json.path("type").asString())
                    && "IMAGE".equals(json.path("message").path("messageType").asString())
                    && imageId.equals(json.path("message").path("imageId").asString());
        }));
    }

    @Test
    void invalidAccessTokenCannotReadChatHistory() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        ScheduledExecutorService scheduler = scheduler();
        ChatWebSocketHandler handler = new ChatWebSocketHandler(
                objectMapper, tokenProvider, userRepository, messageService, scheduler);
        WebSocketSession session = session();
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"AUTH\",\"accessToken\":\"invalid-token\"}"));

        verify(userRepository, never()).findByLoginId(any());
        verify(messageService, never()).getHistory(any(), any());
        verify(session).close(any());
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void validAccessTokenAuthenticatesMemberAndReturnsInitialHistory() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        ScheduledExecutorService scheduler = scheduler();
        ChatWebSocketHandler handler = new ChatWebSocketHandler(
                objectMapper, tokenProvider, userRepository, messageService, scheduler);
        WebSocketSession session = session();
        User user = mock(User.class);

        when(tokenProvider.validateToken("valid-access-token")).thenReturn(true);
        when(tokenProvider.getLoginId("valid-access-token")).thenReturn("member1");
        when(tokenProvider.getExpiration("valid-access-token")).thenReturn(Instant.now().plusSeconds(60));
        when(userRepository.findByLoginId("member1")).thenReturn(java.util.Optional.of(user));
        when(user.getUserId()).thenReturn(1L);
        when(messageService.getHistory(null, null)).thenReturn(new ChatHistoryPage(List.of(), false, null));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"AUTH\",\"accessToken\":\"valid-access-token\"}"));

        verify(messageService).getHistory(null, null);
        verify(session, times(3)).sendMessage(any(TextMessage.class));
    }

    @Test
    void countsDistinctAuthenticatedMembersAndUpdatesWhenTheirLastSessionCloses() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        ChatWebSocketHandler handler = new ChatWebSocketHandler(
                objectMapper, tokenProvider, userRepository, messageService, scheduler());
        WebSocketSession firstTab = session();
        WebSocketSession secondTab = session();
        WebSocketSession anotherMember = session();
        User member = mock(User.class);
        User observer = mock(User.class);

        when(tokenProvider.validateToken(any())).thenReturn(true);
        when(tokenProvider.getLoginId("member-tab-1")).thenReturn("member1");
        when(tokenProvider.getLoginId("member-tab-2")).thenReturn("member1");
        when(tokenProvider.getLoginId("observer-token")).thenReturn("member2");
        when(tokenProvider.getExpiration(any())).thenReturn(Instant.now().plusSeconds(60));
        when(userRepository.findByLoginId("member1")).thenReturn(java.util.Optional.of(member));
        when(userRepository.findByLoginId("member2")).thenReturn(java.util.Optional.of(observer));
        when(member.getUserId()).thenReturn(1L);
        when(observer.getUserId()).thenReturn(2L);
        when(messageService.getHistory(null, null)).thenReturn(new ChatHistoryPage(List.of(), false, null));

        handler.handleTextMessage(firstTab, authFrame("member-tab-1"));
        handler.handleTextMessage(secondTab, authFrame("member-tab-2"));
        handler.handleTextMessage(anotherMember, authFrame("observer-token"));
        handler.afterConnectionClosed(firstTab, CloseStatus.NORMAL);
        handler.afterConnectionClosed(secondTab, CloseStatus.NORMAL);

        ArgumentCaptor<TextMessage> frames = ArgumentCaptor.forClass(TextMessage.class);
        verify(anotherMember, times(5)).sendMessage(frames.capture());
        List<Integer> presenceCounts = frames.getAllValues().stream()
                .map(frame -> {
                    try {
                        return objectMapper.readTree(frame.getPayload());
                    } catch (Exception exception) {
                        throw new AssertionError(exception);
                    }
                })
                .filter(frame -> "PRESENCE".equals(frame.path("type").asString()))
                .map(frame -> frame.path("onlineCount").asInt())
                .toList();

        assertEquals(List.of(2, 2, 1), presenceCounts);
    }

    @Test
    void tokenExpiryClosesAuthenticatedConnectionAndBlocksFurtherCommands() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        ScheduledExecutorService scheduler = scheduler();
        ChatWebSocketHandler handler = new ChatWebSocketHandler(
                objectMapper, tokenProvider, userRepository, messageService, scheduler);
        WebSocketSession session = session();
        User user = mock(User.class);

        when(tokenProvider.validateToken("valid-access-token")).thenReturn(true, true, true, false);
        when(tokenProvider.getLoginId("valid-access-token")).thenReturn("member1");
        when(tokenProvider.getExpiration("valid-access-token")).thenReturn(Instant.now().plusSeconds(60));
        when(userRepository.findByLoginId("member1")).thenReturn(java.util.Optional.of(user));
        when(user.getUserId()).thenReturn(1L);
        when(messageService.getHistory(null, null)).thenReturn(new ChatHistoryPage(List.of(), false, null));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"AUTH\",\"accessToken\":\"valid-access-token\"}"));
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"SEND\",\"content\":\"만료 후 메시지\"}"));

        verify(messageService, never()).send(any(), any());
        verify(session).close(any());
    }

    @Test
    void unauthenticatedConnectionIsClosedAfterAuthenticationDeadline() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        ScheduledExecutorService scheduler = scheduler();
        ChatWebSocketHandler handler = new ChatWebSocketHandler(
                objectMapper, tokenProvider, userRepository, messageService, scheduler);
        WebSocketSession session = session();
        ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);

        handler.afterConnectionEstablished(session);
        verify(scheduler).schedule(timeout.capture(), org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
        timeout.getValue().run();

        verify(session).close(any());
    }

    @Test
    void authenticatedSocketIsClosedAtAccessTokenExpiry() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        TokenProvider tokenProvider = mock(TokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatMessageService messageService = mock(ChatMessageService.class);
        ScheduledExecutorService scheduler = scheduler();
        ChatWebSocketHandler handler = new ChatWebSocketHandler(
                objectMapper, tokenProvider, userRepository, messageService, scheduler);
        WebSocketSession session = session();
        User user = mock(User.class);
        ArgumentCaptor<Runnable> expiry = ArgumentCaptor.forClass(Runnable.class);

        when(tokenProvider.validateToken("valid-access-token")).thenReturn(true);
        when(tokenProvider.getLoginId("valid-access-token")).thenReturn("member1");
        when(tokenProvider.getExpiration("valid-access-token")).thenReturn(Instant.now().plusSeconds(60));
        when(userRepository.findByLoginId("member1")).thenReturn(java.util.Optional.of(user));
        when(user.getUserId()).thenReturn(1L);
        when(messageService.getHistory(null, null)).thenReturn(new ChatHistoryPage(List.of(), false, null));

        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"AUTH\",\"accessToken\":\"valid-access-token\"}"));
        verify(scheduler).schedule(expiry.capture(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MILLISECONDS));
        expiry.getValue().run();

        verify(session).close(any());
    }

    private ScheduledExecutorService scheduler() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).schedule(any(Runnable.class), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(TimeUnit.class));
        return scheduler;
    }

    private WebSocketSession session() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private TextMessage authFrame(String token) {
        return new TextMessage("{\"type\":\"AUTH\",\"accessToken\":\"" + token + "\"}");
    }
}
