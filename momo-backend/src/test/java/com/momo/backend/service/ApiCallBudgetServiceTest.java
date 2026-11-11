package com.momo.backend.service;

import com.momo.backend.entity.ApiCallBudgetState;
import com.momo.backend.repository.ApiCallBudgetStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiCallBudgetServiceTest {

    @Mock
    private ApiCallBudgetStateRepository repository;

    private ApiCallBudgetService service;
    private ApiCallBudgetState[] savedState;

    @BeforeEach
    void setUp() {
        service = new ApiCallBudgetService(repository);
        ReflectionTestUtils.setField(service, "kobisDailyLimit", 1_000L);
        ReflectionTestUtils.setField(service, "kmdbDailyLimit", 1_000L);
        ReflectionTestUtils.setField(service, "tmdbDailyLimit", 1_000L);
        savedState = new ApiCallBudgetState[1];

        when(repository.insertIfAbsent(anyString(), anyString(), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    if (savedState[0] == null) {
                        savedState[0] = ApiCallBudgetState.create(
                                invocation.getArgument(1),
                                invocation.getArgument(2)
                        );
                    }
                    return 1;
                });
        when(repository.findByIdForUpdate(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(savedState[0]));
        when(repository.save(any(ApiCallBudgetState.class)))
                .thenAnswer(invocation -> {
                    savedState[0] = invocation.getArgument(0);
                    return savedState[0];
                });
    }

    @Test
    void createsBudgetRowBeforeLockingAndUpdatingIt() {
        service.acquire("KOBIS", ApiCallBudgetService.TrafficType.PREFETCH);

        InOrder order = inOrder(repository);
        order.verify(repository).insertIfAbsent(
                startsWith("KOBIS_"), eq("KOBIS"), any(LocalDate.class));
        order.verify(repository).findByIdForUpdate(startsWith("KOBIS_"));
        order.verify(repository).save(any(ApiCallBudgetState.class));
        assertEquals(1L, savedState[0].getPrefetchCalls());
    }

    @Test
    void prefetchCannotConsumeReservedUserSearchBudget() {
        for (int i = 0; i < 900; i++) {
            service.acquire("KOBIS", ApiCallBudgetService.TrafficType.PREFETCH);
        }

        assertThrows(ApiCallBudgetService.ApiBudgetExceededException.class,
                () -> service.acquire("KOBIS", ApiCallBudgetService.TrafficType.PREFETCH));

        // 사전 수집이 90%를 모두 사용해도 검색용 10%는 남아 있어야 한다.
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
}
