package com.momo.backend.service;

import com.momo.backend.repository.ApiCallBudgetStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiCallBudgetServiceTest {

    @Mock
    private ApiCallBudgetStateRepository repository;

    private ApiCallBudgetService service;
    private long prefetchUsed;
    private long demandUsed;

    @BeforeEach
    void setUp() {
        service = new ApiCallBudgetService(repository);
        ReflectionTestUtils.setField(service, "kobisDailyLimit", 1_000L);
        ReflectionTestUtils.setField(service, "kmdbDailyLimit", 1_000L);
        ReflectionTestUtils.setField(service, "tmdbDailyLimit", 1_000L);
        when(repository.insertIfAbsent(anyString(), anyString(), any(LocalDate.class))).thenReturn(1);
        when(repository.incrementPrefetchIfAvailable(anyString(), anyLong())).thenAnswer(invocation -> {
            long limit = invocation.getArgument(1);
            if (prefetchUsed >= limit) return 0;
            prefetchUsed++;
            return 1;
        });
        when(repository.incrementDemandIfAvailable(anyString(), anyLong())).thenAnswer(invocation -> {
            long limit = invocation.getArgument(1);
            if (demandUsed >= limit) return 0;
            demandUsed++;
            return 1;
        });
    }

    @Test
    void insertsRowThenUsesAtomicCounterUpdate() {
        service.acquire("KOBIS", ApiCallBudgetService.TrafficType.PREFETCH);
        InOrder order = inOrder(repository);
        order.verify(repository).insertIfAbsent(startsWith("KOBIS_"), eq("KOBIS"), any(LocalDate.class));
        order.verify(repository).incrementPrefetchIfAvailable(startsWith("KOBIS_"), 900L);
    }

    @Test
    void prefetchCannotConsumeReservedUserSearchBudget() {
        for (int i = 0; i < 900; i++) {
            service.acquire("KOBIS", ApiCallBudgetService.TrafficType.PREFETCH);
        }
        assertThrows(ApiCallBudgetService.ApiBudgetExceededException.class,
                () -> service.acquire("KOBIS", ApiCallBudgetService.TrafficType.PREFETCH));
        service.acquire("KOBIS", ApiCallBudgetService.TrafficType.USER_SEARCH);
    }

    @Test
    void userSearchCannotConsumeReservedPrefetchBudget() {
        for (int i = 0; i < 100; i++) {
            service.acquire("KOBIS", ApiCallBudgetService.TrafficType.USER_SEARCH);
        }
        assertThrows(ApiCallBudgetService.ApiBudgetExceededException.class,
                () -> service.acquire("KOBIS", ApiCallBudgetService.TrafficType.USER_SEARCH));
        service.acquire("KOBIS", ApiCallBudgetService.TrafficType.PREFETCH);
    }

    @Test
    void unknownDailyLimitStillCountsWithoutInventingACap() {
        ReflectionTestUtils.setField(service, "kmdbDailyLimit", 0L);
        service.acquire("KMDB", ApiCallBudgetService.TrafficType.PREFETCH);
        verify(repository).incrementPrefetchIfAvailable(startsWith("KMDB_"), Long.MAX_VALUE);
    }
}
