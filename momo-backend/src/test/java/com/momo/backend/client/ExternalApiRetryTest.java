package com.momo.backend.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

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
}
