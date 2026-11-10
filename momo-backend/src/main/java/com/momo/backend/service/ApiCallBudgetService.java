package com.momo.backend.service;

import com.momo.backend.entity.ApiCallBudgetState;
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

    @Value("${momo.api-budget.kobis.daily-limit:1000}")
    private long kobisDailyLimit;

    @Value("${momo.api-budget.kmdb.daily-limit:1000}")
    private long kmdbDailyLimit;

    @Value("${momo.api-budget.tmdb.daily-limit:1000}")
    private long tmdbDailyLimit;

    @Transactional
    public synchronized void acquire(String provider, TrafficType trafficType) {
        String normalizedProvider = provider.toUpperCase(Locale.ROOT);
        long dailyLimit = dailyLimitFor(normalizedProvider);
        LocalDate today = LocalDate.now(KOREA_ZONE);
        ApiCallBudgetState state = repository.findById(normalizedProvider + "_" + today)
                .orElseGet(() -> ApiCallBudgetState.create(normalizedProvider, today));

        long prefetchLimit = (long) Math.floor(dailyLimit * PREFETCH_RATIO);
        long demandLimit = dailyLimit - prefetchLimit;

        if (trafficType == TrafficType.PREFETCH) {
            if (state.getPrefetchCalls() >= prefetchLimit) {
                throw new ApiBudgetExceededException(normalizedProvider,
                        "사전 수집 예산(90%)을 모두 사용했습니다.");
            }
            state.increasePrefetch();
        } else {
            if (state.getDemandCalls() >= demandLimit) {
                throw new ApiBudgetExceededException(normalizedProvider,
                        "사용자 검색 예산(10%)을 모두 사용했습니다.");
            }
            state.increaseDemand();
        }

        repository.save(state);
    }

    private long dailyLimitFor(String provider) {
        long limit = switch (provider) {
            case "KOBIS" -> kobisDailyLimit;
            case "KMDB" -> kmdbDailyLimit;
            case "TMDB" -> tmdbDailyLimit;
            default -> throw new IllegalArgumentException("지원하지 않는 API 공급처: " + provider);
        };
        if (limit < 1) {
            throw new IllegalStateException(provider + " 일일 API 예산이 1 이상으로 설정되어야 합니다.");
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
