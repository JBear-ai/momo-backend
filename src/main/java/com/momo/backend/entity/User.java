package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    // 회원을 구분하는 고유 번호(PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // 로그인할 때 사용하는 아이디
    @Column(name = "login_id", nullable = false, unique = true, length = 20)
    private String loginId;

    // 암호화된 비밀번호를 저장
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // 화면에 표시할 회원 닉네임
    @Column(nullable = false, unique = true, length = 12)
    private String nickname;

    // 회원이 필수로 등록하는 이메일
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    // 회원 정보가 처음 저장된 날짜와 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        // 회원 정보가 DB에 처음 저장될 때 현재 시간을 기록
        this.createdAt = LocalDateTime.now();
    }
    // 회원가입 시 입력받은 정보로 회원 객체를 생성
    public User(String loginId, String passwordHash,
                String nickname, String email) {
        this.loginId = loginId;
        this.passwordHash = passwordHash; // 암호화가 목적
        this.nickname = nickname;
        this.email = email;
    }


    // 회원의 닉네임과 이메일 수정
    public void updateProfile(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
    }

    // 회원 비밀번호 수정
    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }


}