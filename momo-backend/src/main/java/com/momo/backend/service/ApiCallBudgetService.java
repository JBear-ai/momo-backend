package com.momo.backend.service;

import com.momo.backend.repository.ApiCallBudgetStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * 공급처별 일일 호출 예산을 DB에 기록한다.
 * 사전 수집 90%, 사용자 검색 10%를 서로 침범하지 않는 예약 예산으로 운용한다.
 * daily-limit 값은 공급처의 공식 한도가 아니라 운영자가 설정하는 MOMO 자체 예산이다.
 */
@Service
@RequiredArgsConstructor
public class ApiCallBudgetService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final double PREFETCH_RATIO = 0.90;

    private final ApiCallBudgetStateRepository repository;

    // 0 means the provider's daily quota is unknown or is not a fixed daily quota.
    // Do not invent a local daily cap in that case.
    @Value("${momo.api-budget.kobis.daily-limit:0}")
    private long kobisDailyLimit;

    @Value("${momo.api-budget.kmdb.daily-limit:0}")
    private long kmdbDailyLimit;

    @Value("${momo.api-budget.tmdb.daily-limit:0}")
    private long tmdbDailyLimit;

    @Transactional
    public void acquire(String provider, TrafficType trafficType) {
        String normalizedProvider = provider.toUpperCase(Locale.ROOT);
        long dailyLimit = dailyLimitFor(normalizedProvider);
        LocalDate today = LocalDate.now(KOREA_ZONE);
        String budgetId = normalizedProvider + "_" + today;

        // INSERT IGNORE는 동일 날짜/공급처 행의 최초 생성 경쟁을 DB에서 원자적으로 처리한다.
        // 트랜잭션이 커밋되기 전에 Java synchronized 락이 풀리는 기존 구조의 경합을 제거한다.
        repository.insertIfAbsent(budgetId, normalizedProvider, today);

        // 90:10 예약 한도. 공급처 한도가 설정되지 않으면 호출량만 집계한다.
        long maxCalls = Long.MAX_VALUE;
        if (dailyLimit > 0) {
            long prefetchLimit = (long) Math.floor(dailyLimit * PREFETCH_RATIO);
            maxCalls = trafficType == TrafficType.PREFETCH
                    ? prefetchLimit : dailyLimit - prefetchLimit;
        }

        // DB 단일 UPDATE가 증가와 한도 검사를 원자적으로 수행한다.
        int updated = trafficType == TrafficType.PREFETCH
                ? repository.incrementPrefetchIfAvailable(budgetId, maxCalls)
                : repository.incrementDemandIfAvailable(budgetId, maxCalls);

        if (updated != 1) {
            String message = trafficType == TrafficType.PREFETCH
                    ? "사전 수집 예산(90%)을 모두 사용했습니다."
                    : "사용자 검색 예산(10%)을 모두 사용했습니다.";
            throw new ApiBudgetExceededException(normalizedProvider, message);
        }
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, BudgetSnapshot> getTodayStatus() {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        java.util.Map<String, BudgetSnapshot> result = new java.util.LinkedHashMap<>();
        for (String provider : java.util.List.of("KOBIS", "KMDB", "TMDB")) {
            long limit = dailyLimitFor(provider);
            ApiCallBudgetState state = repository.findById(provider + "_" + today)
                    .orElseGet(() -> ApiCallBudgetState.create(provider, today));
            long prefetchLimit = limit > 0
                    ? (long) Math.floor(limit * PREFETCH_RATIO) : 0;
            long demandLimit = limit > 0 ? limit - prefetchLimit : 0;
            result.put(provider, new BudgetSnapshot(
                    limit,
                    prefetchLimit,
                    demandLimit,
                    state.getPrefetchCalls(),
                    state.getDemandCalls(),
                    limit > 0
            ));
        }
        return java.util.Map.copyOf(result);
    }

    public record BudgetSnapshot(
            long dailyLimit,
            long prefetchLimit,
            long userSearchLimit,
            long prefetchUsed,
            long userSearchUsed,
            boolean dailyLimitConfigured
    ) {}

    private long dailyLimitFor(String provider) {
        long limit = switch (provider) {
            case "KOBIS" -> kobisDailyLimit;
            case "KMDB" -> kmdbDailyLimit;
            case "TMDB" -> tmdbDailyLimit;
            default -> throw new IllegalArgumentException("지원하지 않는 API 공급처: " + provider);
        };
        if (limit < 0) {
            throw new IllegalStateException(provider + " 일일 API 예산은 0 또는 양수여야 합니다.");
        }
        return limit;
    }

    public enum TrafficType {
        PREFETCH,
        USER_SEARCH
    }

    public static class ApiBudgetExceededException extends RuntimeException {
        public ApiBudgetExceededException(String provider, String message) {
            super(provider + " API 호출 제한: " + message);
        }
    }
}
