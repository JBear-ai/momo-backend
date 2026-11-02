
package com.momo.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "posterTaskExecutor")
    public Executor posterTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 동시에 처리할 포스터 검색 작업
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(8);

        // 대기 작업
        executor.setQueueCapacity(100);

        // 스레드 이름
        executor.setThreadNamePrefix("poster-");

        // 서버 종료 시 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        return executor;
    }
}
