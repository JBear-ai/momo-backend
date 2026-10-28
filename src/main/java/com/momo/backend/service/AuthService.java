package com.momo.backend.service;

import com.momo.backend.dto.auth.SignupRequest;
import com.momo.backend.dto.auth.UserResponse;
import com.momo.backend.entity.User;
import com.momo.backend.repository.UserRepository;
import com.momo.backend.dto.auth.UserUpdateRequest;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.entity.Review;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.CommentReactionRepository;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    // 회원 DB 접근과 비밀번호 암호화에 필요한 객체를 Spring에서 주입받음
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원 탈퇴시 리뷰와 댓글 삭제를 위한
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;

    // 회원 탈퇴시 리뷰와 댓글 좋아요/싫어요 데이터 접근
    private final ReviewReactionRepository reviewReactionRepository;
    private final CommentReactionRepository commentReactionRepository;

    // 로그인 아이디가 이미 사용 중인지 확인
    public boolean isMember(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    // 회원가입 처리
    public UserResponse signUp(SignupRequest dto) {

        // 로그인 아이디 중복 확인
        if (userRepository.existsByLoginId(dto.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 닉네임 중복 확인
        if (userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 입력받은 회원 정보를 Entity로 변환
        User user = toEntity(dto);

        // 회원 정보를 DB에 저장
        User savedUser = userRepository.save(user);

        // 저장된 회원 정보를 응답 DTO로 변환
        return new UserResponse(savedUser);
    }

    // 회원가입 DTO를 User Entity로 변환
    private User toEntity(SignupRequest dto) {

        // 비밀번호를 암호화한 뒤 User 객체 생성
        return new User(
                dto.getLoginId(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getNickname(),
                dto.getEmail()
        );
    }

    // 로그인 아이디와 비밀번호가 일치하는지 확인
    public User login(String loginId, String password) {

        // 로그인 아이디로 회원 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        // 회원 조회 성공 확인
        log.info("로그인 회원 조회 성공: {}", user.getLoginId());

        // 비밀번호 일치 여부 확인
        boolean passwordMatches =
                passwordEncoder.matches(password, user.getPasswordHash());

        log.info("비밀번호 일치 여부: {}", passwordMatches);

        if (!passwordMatches) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 로그인 성공 시 회원 반환
        return user;
    }

    // 로그인 아이디로 현재 로그인한 회원 정보 조회
    @Transactional(readOnly = true)
    public User getLoginUser(String loginId) {

        return userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }


    // 현재 로그인한 회원의 닉네임과 이메일 수정
    public UserResponse updateMyInfo(String loginId, UserUpdateRequest dto) {

        // 로그인 아이디로 현재 회원 정보 조회
        User user = getLoginUser(loginId);

        // 닉네임을 변경할 경우 다른 회원과 중복되는지 확인
        if (!user.getNickname().equals(dto.nickname())
                && userRepository.existsByNickname(dto.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 입력받은 닉네임과 이메일로 회원 정보 수정
        user.updateProfile(dto.nickname(), dto.email());

        // 수정된 회원 정보를 응답 DTO로 변환
        return new UserResponse(user);
    }


    // 회원 탈퇴 시 작성한 댓글과 리뷰, 좋아요/싫어요를 모두 삭제
    public void deleteMyAccount(String loginId) {

        // 현재 로그인한 회원 조회
        User user = getLoginUser(loginId);
        Long userId = user.getUserId();

        // 탈퇴 회원이 남긴 댓글 좋아요/싫어요 삭제
        commentReactionRepository.deleteByUser_UserId(userId);

        // 탈퇴 회원이 작성한 댓글에 달린 좋아요/싫어요 삭제
        commentReactionRepository.deleteByComment_User_UserId(userId);

        // 탈퇴 회원이 남긴 리뷰 좋아요/싫어요 삭제
        reviewReactionRepository.deleteByUser_UserId(userId);

        // 탈퇴 회원이 작성한 댓글 삭제
        commentRepository.deleteByUser_UserId(userId);

        // 탈퇴 회원이 작성한 리뷰 조회
        List<Review> reviews = reviewRepository.findAllByUser_UserId(userId);

        // 탈퇴 회원의 리뷰에 연결된 데이터 삭제
        for (Review review : reviews) {

            Long reviewId = review.getReviewId();

            // 리뷰에 달린 댓글의 좋아요/싫어요 삭제
            commentReactionRepository.deleteByComment_Review_ReviewId(reviewId);

            // 리뷰에 달린 댓글 삭제
            commentRepository.deleteByReview_ReviewId(reviewId);

            // 리뷰에 달린 좋아요/싫어요 삭제
            reviewReactionRepository.deleteByReview_ReviewId(reviewId);
        }

        // 탈퇴 회원이 작성한 리뷰 삭제
        reviewRepository.deleteAll(reviews);

        // 회원 정보 삭제
        userRepository.delete(user);
    }


}