package com.momo.backend.repository;

import com.momo.backend.entity.ApiCallBudgetState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

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
            @Param("usageDate") LocalDate usageDate
    );

    /**
     * 예산 카운터를 변경하기 전에 DB 행 잠금을 획득한다.
     * synchronized와 달리 여러 서버 인스턴스 사이에서도 동시 갱신을 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select budget from ApiCallBudgetState budget where budget.id = :id")
    Optional<ApiCallBudgetState> findByIdForUpdate(@Param("id") String id);
}
