package com.momo.backend.service;

import com.momo.backend.dto.comment.CommentRequest;
import com.momo.backend.dto.comment.CommentResponse;
import com.momo.backend.entity.Comment;
import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import com.momo.backend.repository.CommentReactionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {

    // 댓글, 리뷰, 회원 DB 접근
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // 댓글 좋아요/싫어요 데이터 접근을 위한 참조
    private final CommentReactionRepository commentReactionRepository;


    // 댓글 작성
    public CommentResponse createComment(
            Long reviewId,
            String loginId,
            CommentRequest dto) {

        // 댓글을 작성할 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 현재 로그인한 회원 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 댓글 Entity 생성 및 저장
        Comment comment = new Comment(
                review,
                user,
                dto.getContent()
        );

        Comment savedComment = commentRepository.save(comment);

        return toResponse(savedComment, user.getUserId());
    }

    // 특정 리뷰의 댓글 목록 조회 (오래된 댓글부터)
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(
            Long reviewId,
            int page,
            String loginId) {

        // 존재하지 않는 리뷰인지 확인
        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("리뷰를 찾을 수 없습니다.");
        }

        // 비회원도 댓글을 읽을 수 있으며, 이 경우 개인 반응은 null로 반환한다.
        Long currentUserId = null;
        if (loginId != null && !loginId.isBlank()) {
            currentUserId = userRepository.findByLoginId(loginId)
                    .map(user -> user == null ? null : user.getUserId())
                    .orElse(null);
        }

        // 한 페이지에 댓글 10개씩 조회
        Pageable pageable = PageRequest.of(page, 10);
        Long viewerId = currentUserId;

        Page<Comment> comments = commentRepository
                .findByReview_ReviewIdOrderByCreatedAtAsc(reviewId, pageable);

        // 페이지에 포함된 댓글의 반응을 한 번에 조회해 댓글별 반복 쿼리를 방지한다.
        List<Long> commentIds = comments.getContent().stream()
                .map(Comment::getCommentId)
                .toList();

        Map<Long, long[]> reactionCounts = new HashMap<>();
        Map<Long, Boolean> myReactions = new HashMap<>();

        if (!commentIds.isEmpty()) {
            List<CommentReactionRepository.CommentReactionSummary> summaries =
                    commentReactionRepository.findReactionSummariesByCommentIds(commentIds);

            for (CommentReactionRepository.CommentReactionSummary summary : summaries) {
                Long commentId = summary.getCommentId();
                long[] counts = reactionCounts.computeIfAbsent(
                        commentId, key -> new long[2]
                );

                if (Boolean.TRUE.equals(summary.getLiked())) {
                    counts[0]++;
                } else {
                    counts[1]++;
                }

                if (viewerId != null && viewerId.equals(summary.getUserId())) {
                    myReactions.put(commentId, summary.getLiked());
                }
            }
        }

        return comments.map(comment -> {
            Long commentId = comment.getCommentId();
            long[] counts = reactionCounts.getOrDefault(commentId, new long[2]);
            User author = comment.getUser();
            boolean profileImageAvailable = author.getProfileImage() != null
                    && !author.getProfileImage().isBlank();

            return new CommentResponse(
                    comment,
                    profileImageAvailable,
                    counts[0],
                    counts[1],
                    myReactions.get(commentId)
            );
        });
    }

    // 본인이 작성한 댓글 수정
    public CommentResponse updateComment(
            Long commentId,
            String loginId,
            CommentRequest dto) {

        // 수정할 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 댓글 작성자와 로그인 사용자 비교
        if (!comment.getUser().getLoginId().equals(loginId)) {
            throw new SecurityException(
                    "본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        // 기존 Comment Entity의 수정 메서드 재사용
        comment.updateComment(dto.getContent());

        return toResponse(comment, comment.getUser().getUserId());
    }

    // 본인이 작성한 댓글 삭제
    public void deleteComment(
            Long commentId,
            String loginId) {

        // 삭제할 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 댓글 작성자와 로그인 사용자 비교
        if (!comment.getUser().getLoginId().equals(loginId)) {
            throw new SecurityException(
                    "본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        // 댓글에 남겨진 좋아요/싫어요 삭제
        commentReactionRepository.deleteByComment_CommentId(commentId);

        // 댓글 삭제
        commentRepository.delete(comment);
    }

    // 댓글 작성자 프로필 및 반응 정보를 공통 응답으로 변환
    private CommentResponse toResponse(Comment comment, Long currentUserId) {
        Long commentId = comment.getCommentId();

        long likeCount = commentReactionRepository
                .countByComment_CommentIdAndLiked(commentId, true);
        long dislikeCount = commentReactionRepository
                .countByComment_CommentIdAndLiked(commentId, false);

        Boolean myReaction = currentUserId == null
                ? null
                : commentReactionRepository
                        .findByUser_UserIdAndComment_CommentId(currentUserId, commentId)
                        .map(reaction -> reaction.getLiked())
                        .orElse(null);

        boolean profileImageAvailable = comment.getUser().getProfileImage() != null
                && !comment.getUser().getProfileImage().isBlank();

        return new CommentResponse(
                comment,
                profileImageAvailable,
                likeCount,
                dislikeCount,
                myReaction
        );
    }
}