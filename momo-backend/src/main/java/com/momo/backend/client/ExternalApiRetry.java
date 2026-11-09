package com.momo.backend.client;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

/**
 * 외부 영화 API의 일시적인 네트워크·서버 장애에 한해 한 번만 재시도한다.
 * 인증 오류나 잘못된 요청과 같은 4xx 오류는 재시도하지 않는다.
 */
final class ExternalApiRetry {

    private static final int MAX_ATTEMPTS = 2;
    private static final long DEFAULT_DELAY_MILLIS = 300;
    private static final long MAX_RETRY_AFTER_MILLIS = 2_000;

    private ExternalApiRetry() {
    }

    static <T> T execute(Supplier<T> request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return request.get();
            } catch (RuntimeException exception) {
                if (attempt >= MAX_ATTEMPTS || !isRetryable(exception)) {
                    throw exception;
                }

                long delayMillis = resolveDelay(exception);
                if (delayMillis < 0) {
                    throw exception;
                }
                sleep(delayMillis);
            }
        }

        throw new IllegalStateException("외부 API 재시도 흐름이 비정상적으로 종료되었습니다.");
    }

    private static boolean isRetryable(RuntimeException exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }

        if (exception instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            return status == 429 || responseException.getStatusCode().is5xxServerError();
        }

        return false;
    }

    private static long resolveDelay(RuntimeException exception) {
        if (exception instanceof RestClientResponseException responseException
                && responseException.getResponseHeaders() != null) {
            String retryAfter = responseException.getResponseHeaders().getFirst("Retry-After");

            if (retryAfter != null) {
                try {
                    long seconds = Long.parseLong(retryAfter.trim());
                    if (seconds < 0) {
                        return DEFAULT_DELAY_MILLIS;
                    }
                    return seconds * 1_000 > MAX_RETRY_AFTER_MILLIS
                            ? -1
                            : seconds * 1_000;
                } catch (NumberFormatException ignored) {
                    try {
                        long delayMillis = Duration.between(
                                Instant.now(),
                                ZonedDateTime.parse(
                                        retryAfter,
                                        DateTimeFormatter.RFC_1123_DATE_TIME
                                ).toInstant()
                        ).toMillis();

                        // 서버가 더 긴 대기 시간을 요구하면 조기 재시도하지 않는다.
                        return delayMillis > MAX_RETRY_AFTER_MILLIS
                                ? -1
                                : Math.max(delayMillis, 0);
                    } catch (DateTimeParseException ignoredDate) {
                        // 해석할 수 없는 Retry-After 값은 기본 대기 시간을 사용한다.
                    }
                }
            }
        }

        return DEFAULT_DELAY_MILLIS;
    }

    private static void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("외부 API 재시도 대기 중 작업이 중단되었습니다.", exception);
        }
    }
}
