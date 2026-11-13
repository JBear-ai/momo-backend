package com.momo.backend.repository;

import com.momo.backend.entity.MovieCatalogSyncState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MovieCatalogSyncStateRepository extends JpaRepository<MovieCatalogSyncState, Integer> {

    // 여러 애플리케이션 인스턴스가 동시에 시작해도 동기화 상태 행을 하나만 생성한다.
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO movie_catalog_sync_state
                (id, phase, next_page, total_pages, total_count, updated_at)
            VALUES
                (1, 'INITIAL', 1, 0, 0, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertInitialIfAbsent();

    // JVM synchronized만으로 막을 수 없는 다중 서버의 중복 카탈로그 수집을 DB 행 잠금으로 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from MovieCatalogSyncState state where state.id = :id")
    Optional<MovieCatalogSyncState> findByIdForUpdate(@Param("id") Integer id);
}
