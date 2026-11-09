package com.momo.backend.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalApiRetryTest {

    @Test
    void retriesOneTimeAfterTransientNetworkFailure() {
        AtomicInteger attempts = new AtomicInteger();

        String result = ExternalApiRetry.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new ResourceAccessException("temporary timeout");
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void doesNotRetryNonTransientFailure() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(
                IllegalArgumentException.class,
                () -> ExternalApiRetry.execute(() -> {
                    attempts.incrementAndGet();
                    throw new IllegalArgumentException("invalid request");
                })
        );

        assertEquals(1, attempts.get());
    }

    @Test
    void stopsAfterMaximumAttemptCount() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(
                ResourceAccessException.class,
                () -> ExternalApiRetry.execute(() -> {
                    attempts.incrementAndGet();
                    throw new ResourceAccessException("persistent timeout");
                })
        );

        assertEquals(2, attempts.get());
    }
    @Test
    void retriesServerErrorOnceWhenRetryAfterIsImmediate() {
        AtomicInteger attempts = new AtomicInteger();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "0");
        RestClientResponseException serverError = new RestClientResponseException(
                "server error", HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable", headers, new byte[0], StandardCharsets.UTF_8);

        String result = ExternalApiRetry.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw serverError;
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void doesNotRetryBeforeLongRetryAfterWindowExpires() {
        AtomicInteger attempts = new AtomicInteger();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "30");
        RestClientResponseException rateLimited = new RestClientResponseException(
                "rate limited", HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests", headers, new byte[0], StandardCharsets.UTF_8);

        assertThrows(RestClientResponseException.class, () ->
                ExternalApiRetry.execute(() -> {
                    attempts.incrementAndGet();
                    throw rateLimited;
                }));

        assertEquals(1, attempts.get());
    }

    @Test
    void doesNotRetryClientErrorsOtherThanRateLimit() {
        AtomicInteger attempts = new AtomicInteger();
        RestClientResponseException badRequest = new RestClientResponseException(
                "bad request", HttpStatus.BAD_REQUEST,
                "Bad Request", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);

        assertThrows(RestClientResponseException.class, () ->
                ExternalApiRetry.execute(() -> {
                    attempts.incrementAndGet();
                    throw badRequest;
                }));

        assertEquals(1, attempts.get());
    }

}
