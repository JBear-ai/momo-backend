package com.momo.backend.config;

import com.momo.backend.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;

/** 단일 공용 채팅 WebSocket endpoint와 허용 Origin을 등록한다. */
@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final List<String> allowedOrigins;

    public ChatWebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            @Value("${momo.cors.allowed-origins:http://localhost:3000,http://192.168.0.166:3000}") String origins) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(origin -> origin.trim())
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}
