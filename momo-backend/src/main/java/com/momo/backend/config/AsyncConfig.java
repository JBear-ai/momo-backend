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

    // 리뷰 목록의 누락 영화 확인은 KOBIS 상세 조회가 필요할 수 있어 별도 비동기 풀에서 처리한다.
    // 큐가 가득 찬 경우 요청 스레드에서 외부 API를 실행하지 않고 요청 등록을 거부한다.
    @Bean(name = "posterEnsureTaskExecutor")
    public Executor posterEnsureTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("poster-ensure-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    // 홈 박스오피스 포스터는 일반 사전 수집 작업과 분리해 먼저 처리한다.
    @Bean(name = "boxOfficePosterTaskExecutor")
    public Executor boxOfficePosterTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(30);
        executor.setThreadNamePrefix("boxoffice-poster-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "posterTaskExecutor")
    public Executor posterTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 외부 영화 API 호출량과 서버 자원 사용량을 제한한다.
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(100);

        // 대기열이 가득 차면 호출 스레드가 직접 처리해 작업 유실 없이 자연스럽게 속도를 제한한다.
        executor.setThreadNamePrefix("poster-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 서버 종료 시 진행 중인 작업에 종료 유예 시간을 제공한다.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
