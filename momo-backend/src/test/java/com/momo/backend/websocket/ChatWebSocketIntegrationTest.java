package com.momo.backend.websocket;

import com.momo.backend.entity.User;
import com.momo.backend.repository.UserRepository;
import com.momo.backend.security.TokenProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** H2와 임시 포트를 사용해 실제 WebSocket handshake, 인증, 기록 및 송신 경로를 확인한다. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:momo-chat-websocket;DB_CLOSE_DELAY=-1;MODE=MySQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "jwt.secret=TW9tb1Rlc3RPbmx5U2VjcmV0S2V5Rm9ySlZUVGVzdHMxMjM0NQ==",
        })
@ActiveProfiles("test")
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticatedMemberReceivesHistoryAndCanBroadcastUnicodeMessage() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = userRepository.saveAndFlush(
                new User("chat" + suffix.substring(0, 12), "test-password-hash", "채팅" + suffix.substring(0, 8),
                        suffix.substring(0, 12) + "@example.test"));
        String accessToken = tokenProvider.createToken(user.getLoginId());
        BlockingQueue<String> incomingFrames = new LinkedBlockingQueue<>();
        WebSocket socket = null;

        try {
            socket = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + port + "/ws/chat"),
                            new WebSocket.Listener() {
                                @Override
                                public void onOpen(WebSocket webSocket) {
                                    webSocket.request(1);
                                }

                                @Override
                                public java.util.concurrent.CompletionStage<?> onText(
                                        WebSocket webSocket, CharSequence data, boolean last) {
                                    if (last) {
                                        incomingFrames.offer(data.toString());
                                    }
                                    webSocket.request(1);
                                    return null;
                                }
                            })
                    .get(5, TimeUnit.SECONDS);

            socket.sendText("{\"type\":\"AUTH\",\"accessToken\":\"" + accessToken + "\"}", true)
                    .get(5, TimeUnit.SECONDS);
            String authenticated = incomingFrames.poll(5, TimeUnit.SECONDS);
            String initialHistory = incomingFrames.poll(5, TimeUnit.SECONDS);
            String presence = incomingFrames.poll(5, TimeUnit.SECONDS);
            assertNotNull(authenticated);
            assertTrue(authenticated.contains("\"type\":\"AUTHENTICATED\""));
            assertTrue(authenticated.contains("\"userId\":" + user.getUserId()));
            assertNotNull(initialHistory);
            assertTrue(initialHistory.contains("\"type\":\"HISTORY\""));
            assertNotNull(presence);
            assertTrue(presence.contains("\"type\":\"PRESENCE\""));
            assertTrue(presence.contains("\"onlineCount\":1"));

            socket.sendText("{\"type\":\"SEND\",\"content\":\"안녕하세요 😀\"}", true)
                    .get(5, TimeUnit.SECONDS);
            String message = incomingFrames.poll(5, TimeUnit.SECONDS);
            assertNotNull(message);
            assertTrue(message.contains("\"type\":\"MESSAGE\""));
            assertTrue(message.contains("안녕하세요 😀"));
            assertTrue(message.contains(user.getNickname()));

            JsonNode messageFrame = objectMapper.readTree(message);
            long messageId = messageFrame.path("message").path("id").longValue();
            socket.sendText("{\"type\":\"SYNC\",\"afterId\":0,\"limit\":50}", true)
                    .get(5, TimeUnit.SECONDS);
            String synchronization = incomingFrames.poll(5, TimeUnit.SECONDS);
            assertNotNull(synchronization);
            assertTrue(synchronization.contains("\"type\":\"SYNC\""));
            assertTrue(synchronization.contains("\"id\":" + messageId));

            socket.sendText("{\"type\":\"DELETE\",\"messageId\":" + messageId + "}", true)
                    .get(5, TimeUnit.SECONDS);
            String deletion = incomingFrames.poll(5, TimeUnit.SECONDS);
            assertNotNull(deletion);
            assertTrue(deletion.contains("\"type\":\"MESSAGE_DELETED\""));
            assertTrue(deletion.contains("\"messageId\":" + messageId));
        } finally {
            if (socket != null && !socket.isOutputClosed()) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").get(5, TimeUnit.SECONDS);
            }
            userRepository.deleteById(user.getUserId());
        }
    }
}
