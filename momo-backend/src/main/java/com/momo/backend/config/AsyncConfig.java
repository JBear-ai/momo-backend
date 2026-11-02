
package com.momo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // 포스터 조회 전용 비동기 실행 환경
    @Bean(name = "posterTaskExecutor")
    public Executor posterTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 작업 스레드 수
        executor.setCorePoolSize(3);

        // 최대 작업 스레드 수
        executor.setMaxPoolSize(3);

        // 대기 가능한 작업 수
        executor.setQueueCapacity(100);

        // 스레드 이름
        executor.setThreadNamePrefix("poster-");

        // 종료 시 진행 중인 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 종료 대기 시간
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        return executor;
    }
}
