package com.momo.backend.repository;

import com.momo.backend.entity.ApiCallBudgetState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ApiCallBudgetStateRepository extends JpaRepository<ApiCallBudgetState, String> {

        /**
         * 날짜별 예산 행을 원자적으로 생성한다.
         * 여러 인스턴스가 동시에 최초 호출하더라도 PK 중복 예외를 발생시키지 않는다.
         */
        @Modifying
        @Query(value = """
                        INSERT IGNORE INTO api_call_budget
                            (id, provider, usage_date, prefetch_calls, demand_calls)
                        VALUES (:id, :provider, :usageDate, 0, 0)
                        """, nativeQuery = true)
        int insertIfAbsent(
                        @Param("id") String id,
                        @Param("provider") String provider,
                        @Param("usageDate") LocalDate usageDate);

        /**
         * 단일 조건부 UPDATE로 카운터를 원자적으로 증가시킨다.
         * maxCalls는 예약 한도이며 Long.MAX_VALUE는 사실상 무제한 집계다.
         */
        @Modifying
        @Query(value = """
                        UPDATE api_call_budget
                           SET prefetch_calls = prefetch_calls + 1
                         WHERE id = :id AND prefetch_calls < :maxCalls
                        """, nativeQuery = true)
        int incrementPrefetchIfAvailable(@Param("id") String id, @Param("maxCalls") long maxCalls);

        @Modifying
        @Query(value = """
                        UPDATE api_call_budget
                           SET demand_calls = demand_calls + 1
                         WHERE id = :id AND demand_calls < :maxCalls
                        """, nativeQuery = true)
        int incrementDemandIfAvailable(@Param("id") String id, @Param("maxCalls") long maxCalls);
}
