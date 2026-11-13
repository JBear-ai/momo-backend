package com.momo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** 채팅 연결의 인증 및 Access Token 만료 타이머를 관리한다. */
@Configuration
public class ChatWebSocketSchedulerConfig {

    /** 채팅 세션별 인증 제한과 JWT 만료 작업을 수행할 데몬 스케줄러를 제공한다. */
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService chatWebSocketScheduler() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "momo-chat-session-timeouts");
            thread.setDaemon(true);
            return thread;
        });
    }
}
