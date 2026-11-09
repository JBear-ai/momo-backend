package com.momo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "posterTaskExecutor")
    public Executor posterTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 외부 영화 API 호출량과 서버 자원 사용량을 제한한다.
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(100);

        // 작업 추적과 스레드 풀 포화 시 명시적인 복구 처리를 위해 거부 정책을 지정한다.
        executor.setThreadNamePrefix("poster-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        // 서버 종료 시 진행 중인 작업에 종료 유예 시간을 제공한다.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
