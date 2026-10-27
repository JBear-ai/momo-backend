package com.momo.backend.repository;

import com.momo.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    // 로그인 아이디로 회원 정보 조회
    Optional<User> findByLoginId(String loginId);

    // 회원가입 시 로그인 아이디 중복 확인
    boolean existsByLoginId(String loginId);

    // 회원가입 시 닉네임 중복 확인
    boolean existsByNickname(String nickname);
}
