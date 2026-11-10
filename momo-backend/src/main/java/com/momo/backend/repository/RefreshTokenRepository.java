package com.momo.backend.repository;

import com.momo.backend.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUser_UserId(Long userId);

    // Rotation 중 같은 활성 토큰이 동시에 두 번 사용되지 않도록 DB 행을 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.user.userId = :userId")
    Optional<RefreshToken> findByUserIdForUpdate(@Param("userId") Long userId);

    void deleteByUser_UserId(Long userId);

    void deleteByUser_LoginId(String loginId);
}
