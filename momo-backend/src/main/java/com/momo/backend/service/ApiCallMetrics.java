package com.momo.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * 외부 영화 API 호출을 공통 형식으로 집계한다.
 * 프로세스 메모리 기반 집계이며 재시작 시 초기화된다.
 */
@Component
@Slf4j
public class ApiCallMetrics {

    private final Map<String, Counters> counters = new ConcurrentHashMap<>();

    public <T> T record(String provider, String endpoint, Supplier<T> request) {
        String key = provider + "." + endpoint;
        Counters current = counters.computeIfAbsent(key, ignored -> new Counters());
        long startedAt = System.nanoTime();
        current.calls.increment();

        try {
            T result = request.get();
            current.successes.increment();
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            current.totalMillis.add(elapsedMillis);
            log.info("EXTERNAL_API provider={} endpoint={} result=SUCCESS durationMs={} calls={} successes={} failures={}",
                    provider, endpoint, elapsedMillis, current.calls.sum(),
                    current.successes.sum(), current.failures.sum());
            return result;
        } catch (RuntimeException exception) {
            current.failures.increment();
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            current.totalMillis.add(elapsedMillis);
            log.warn("EXTERNAL_API provider={} endpoint={} result=FAILURE durationMs={} calls={} successes={} failures={}",
                    provider, endpoint, elapsedMillis, current.calls.sum(),
                    current.successes.sum(), current.failures.sum(), exception);
            throw exception;
        }
    }

    public Map<String, Snapshot> snapshot() {
        Map<String, Snapshot> result = new java.util.TreeMap<>();
        counters.forEach((key, value) -> result.put(key,
                new Snapshot(value.calls.sum(), value.successes.sum(),
                        value.failures.sum(), value.totalMillis.sum())));
        return Map.copyOf(result);
    }

    public record Snapshot(long calls, long successes, long failures, long totalMillis) {}

    private static final class Counters {
        private final LongAdder calls = new LongAdder();
        private final LongAdder successes = new LongAdder();
        private final LongAdder failures = new LongAdder();
        private final LongAdder totalMillis = new LongAdder();
    }
}