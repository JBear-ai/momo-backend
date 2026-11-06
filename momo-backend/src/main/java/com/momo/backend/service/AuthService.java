package com.momo.backend.service;

import java.io.IOException;
import java.util.List;

import com.momo.backend.dto.auth.SignupRequest;
import com.momo.backend.dto.auth.UserResponse;
import com.momo.backend.dto.auth.UserUpdateRequest;
import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final FirebaseStorageService firebaseStorageService;

    // 회원가입

    public boolean isMember(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    public UserResponse signUp(SignupRequest dto) {
        if (userRepository.existsByLoginId(dto.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if (userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        User user = new User(
                dto.getLoginId(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getNickname(),
                dto.getEmail()
        );

        return new UserResponse(userRepository.save(user));
    }

    // 로그인 및 회원 정보 조회

    public User login(String loginId, String password) {
        User user = getLoginUser(loginId);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public User getLoginUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    public boolean checkLoginIdExists(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    // 회원 정보 수정

    public UserResponse updateMyInfo(String loginId, UserUpdateRequest dto) {
        User user = getLoginUser(loginId);

        if (!user.getNickname().equals(dto.nickname())
                && userRepository.existsByNickname(dto.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        boolean changePassword =
                (dto.currentPassword() != null && !dto.currentPassword().isEmpty())
                || (dto.newPassword() != null && !dto.newPassword().isEmpty());

        if (changePassword) {
            if (dto.currentPassword() == null || dto.currentPassword().isBlank()) {
                throw new IllegalArgumentException("현재 비밀번호를 입력해 주세요.");
            }

            if (!passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }

            if (dto.newPassword() == null || dto.newPassword().isBlank()) {
                throw new IllegalArgumentException("새 비밀번호를 입력해 주세요.");
            }

            String passwordPattern =
                    "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$";

            if (!dto.newPassword().matches(passwordPattern)) {
                throw new IllegalArgumentException(
                        "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자로 입력해 주세요.");
            }

            user.updatePassword(passwordEncoder.encode(dto.newPassword()));
        }

        user.updateProfile(dto.nickname(), dto.email());
        return new UserResponse(user);
    }

    // 내가 누른 좋아요 개수

    @Transactional(readOnly = true)
    public long getMyLikeCount(String loginId) {
        User user = getLoginUser(loginId);

        long reviewLikeCount =
                reviewReactionRepository.countByUser_UserIdAndLiked(user.getUserId(), true);
        long commentLikeCount =
                commentReactionRepository.countByUser_UserIdAndLiked(user.getUserId(), true);

        return reviewLikeCount + commentLikeCount;
    }

    // 회원 탈퇴

    public void deleteMyAccount(String loginId, String password) {
        User user = getLoginUser(loginId);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        Long userId = user.getUserId();

        // 회원이 남긴 반응과 댓글부터 삭제
        commentReactionRepository.deleteByUser_UserId(userId);
        commentReactionRepository.deleteByComment_User_UserId(userId);
        reviewReactionRepository.deleteByUser_UserId(userId);
        commentRepository.deleteByUser_UserId(userId);

        // 회원이 작성한 리뷰에 연결된 댓글과 반응 삭제
        List<Review> reviews = reviewRepository.findAllByUser_UserId(userId);

        for (Review review : reviews) {
            Long reviewId = review.getReviewId();

            commentReactionRepository.deleteByComment_Review_ReviewId(reviewId);
            commentRepository.deleteByReview_ReviewId(reviewId);
            reviewReactionRepository.deleteByReview_ReviewId(reviewId);
        }

        reviewRepository.deleteAll(reviews);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public boolean checkCurrentPassword(String loginId, String currentPassword) {
        User user = getLoginUser(loginId);

        return passwordEncoder.matches(currentPassword, user.getPasswordHash());
    }

    // 프로필 이미지

    public UserResponse updateProfileImage(String loginId, MultipartFile file)
            throws IOException {
        User user = getLoginUser(loginId);
        String oldProfileImage = user.getProfileImage();

        String newProfileImage = firebaseStorageService.uploadProfileImage(file);
        user.updateProfileImage(newProfileImage);

        firebaseStorageService.deleteProfileImage(oldProfileImage);
        return new UserResponse(user);
    }

    public UserResponse removeProfileImage(String loginId) {
        User user = getLoginUser(loginId);

        firebaseStorageService.deleteProfileImage(user.getProfileImage());
        user.removeProfileImage();

        return new UserResponse(user);
    }

    @Transactional(readOnly = true)
    public byte[] getProfileImage(String loginId) {
        User user = getLoginUser(loginId);
        return firebaseStorageService.getProfileImage(user.getProfileImage());
    }

    @Transactional(readOnly = true)
    public String getProfileImageContentType(String loginId) {
        User user = getLoginUser(loginId);
        return firebaseStorageService.getProfileImageContentType(user.getProfileImage());
    }
}
