package com.momo.backend.repository;

import com.momo.backend.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 로그인 아이디로 회원 정보 조회
    Optional<User> findByLoginId(String loginId);

    // 같은 회원의 동시 반응 저장을 직렬화해 중복 INSERT 경쟁을 막는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.loginId = :loginId")
    Optional<User> findByLoginIdForUpdate(@Param("loginId") String loginId);

    // Refresh Token 행이 없는 최초 동시 로그인도 사용자 단위로 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findByUserIdForUpdate(@Param("userId") Long userId);

    // 회원가입 시 로그인 아이디 중복 확인
    boolean existsByLoginId(String loginId);

    // 회원가입 시 닉네임 중복 확인
    boolean existsByNickname(String nickname);
}
