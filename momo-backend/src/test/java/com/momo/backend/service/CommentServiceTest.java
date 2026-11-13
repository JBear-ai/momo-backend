package com.momo.backend.service;

import com.momo.backend.dto.comment.CommentResponse;
import com.momo.backend.dto.comment.CommentRequest;
import com.momo.backend.entity.Comment;
import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createsCommentAfterLockingMemberAndTargetReview() {
        User user = org.mockito.Mockito.mock(User.class);
        Review review = org.mockito.Mockito.mock(Review.class);
        Comment comment = org.mockito.Mockito.mock(Comment.class);
        LocalDateTime now = LocalDateTime.now();

        when(userRepository.findByLoginIdForUpdate("member"))
                .thenReturn(java.util.Optional.of(user));
        when(user.getUserId()).thenReturn(7L);
        when(user.getNickname()).thenReturn("작성자");
        when(user.getProfileImage()).thenReturn(null);
        when(reviewRepository.findByIdForUpdate(15L))
                .thenReturn(java.util.Optional.of(review));
        when(review.getReviewId()).thenReturn(15L);
        when(commentRepository.save(org.mockito.ArgumentMatchers.any(Comment.class)))
                .thenReturn(comment);
        when(comment.getCommentId()).thenReturn(27L);
        when(comment.getReview()).thenReturn(review);
        when(comment.getUser()).thenReturn(user);
        when(comment.getContent()).thenReturn("동시성 검증 댓글");
        when(comment.getCreatedAt()).thenReturn(now);
        when(comment.getUpdatedAt()).thenReturn(now);
        CommentReactionRepository.ReactionCountSummary reactionCounts =
                org.mockito.Mockito.mock(CommentReactionRepository.ReactionCountSummary.class);
        when(reactionCounts.getLikeCount()).thenReturn(0L);
        when(reactionCounts.getDislikeCount()).thenReturn(0L);
        when(commentReactionRepository.findCountsByCommentId(27L))
                .thenReturn(reactionCounts);

        CommentRequest request = new CommentRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(
                request, "content", "동시성 검증 댓글");

        CommentResponse response = commentService.createComment(15L, "member", request);

        assertEquals(27L, response.getCommentId());

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(
                userRepository, reviewRepository, commentRepository);
        order.verify(userRepository).findByLoginIdForUpdate("member");
        order.verify(reviewRepository).findByIdForUpdate(15L);
        order.verify(commentRepository).save(org.mockito.ArgumentMatchers.any(Comment.class));
    }

    @Test
    void updatesCommentAfterLockingItsParentReview() {
        User user = org.mockito.Mockito.mock(User.class);
        Comment commentReference = org.mockito.Mockito.mock(Comment.class);
        Review review = org.mockito.Mockito.mock(Review.class);
        Comment comment = org.mockito.Mockito.mock(Comment.class);

        when(userRepository.findByLoginIdForUpdate("member"))
                .thenReturn(java.util.Optional.of(user));
        when(commentRepository.findById(27L))
                .thenReturn(java.util.Optional.of(commentReference));
        when(commentReference.getReview()).thenReturn(review);
        when(review.getReviewId()).thenReturn(15L);
        when(reviewRepository.findByIdForUpdate(15L))
                .thenReturn(java.util.Optional.of(review));
        when(commentRepository.findByIdForUpdate(27L))
                .thenReturn(java.util.Optional.of(comment));
        when(comment.getUser()).thenReturn(user);
        when(comment.getReview()).thenReturn(review);
        when(comment.getCommentId()).thenReturn(27L);
        when(comment.getContent()).thenReturn("수정 전 댓글");
        when(comment.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(comment.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(user.getUserId()).thenReturn(7L);
        when(user.getNickname()).thenReturn("작성자");
        when(user.getProfileImage()).thenReturn(null);
        when(user.getLoginId()).thenReturn("member");

        CommentRequest request = new CommentRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(
                request, "content", "수정된 댓글");

        commentService.updateComment(27L, "member", request);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(
                userRepository, commentRepository, reviewRepository, comment);
        order.verify(userRepository).findByLoginIdForUpdate("member");
        order.verify(commentRepository).findById(27L);
        order.verify(reviewRepository).findByIdForUpdate(15L);
        order.verify(commentRepository).findByIdForUpdate(27L);
        order.verify(comment).updateComment("수정된 댓글");
    }

    @Test
    void deletesCommentAfterLockingItsParentReview() {
        User user = org.mockito.Mockito.mock(User.class);
        Comment commentReference = org.mockito.Mockito.mock(Comment.class);
        Review review = org.mockito.Mockito.mock(Review.class);
        Comment comment = org.mockito.Mockito.mock(Comment.class);

        when(userRepository.findByLoginIdForUpdate("member"))
                .thenReturn(java.util.Optional.of(user));
        when(commentRepository.findById(27L))
                .thenReturn(java.util.Optional.of(commentReference));
        when(commentReference.getReview()).thenReturn(review);
        when(review.getReviewId()).thenReturn(15L);
        when(reviewRepository.findByIdForUpdate(15L))
                .thenReturn(java.util.Optional.of(review));
        when(commentRepository.findByIdForUpdate(27L))
                .thenReturn(java.util.Optional.of(comment));
        when(comment.getUser()).thenReturn(user);
        when(user.getLoginId()).thenReturn("member");

        commentService.deleteComment(27L, "member");

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(
                userRepository,
                commentRepository,
                reviewRepository,
                commentReactionRepository);
        order.verify(userRepository).findByLoginIdForUpdate("member");
        order.verify(commentRepository).findById(27L);
        order.verify(reviewRepository).findByIdForUpdate(15L);
        order.verify(commentRepository).findByIdForUpdate(27L);
        order.verify(commentReactionRepository).deleteByComment_CommentId(27L);
        order.verify(commentRepository).delete(comment);
    }

    @Test
    void anonymousVisitorCanReadCommentsWithoutPersonalReactionLookup() {
        Long reviewId = 15L;
        Long commentId = 27L;
        LocalDateTime createdAt = LocalDateTime.now();

        Comment comment = org.mockito.Mockito.mock(Comment.class);
        Review review = org.mockito.Mockito.mock(Review.class);
        User author = org.mockito.Mockito.mock(User.class);

        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(commentRepository.findByReview_ReviewIdOrderByCreatedAtAscCommentIdAsc(
                eq(reviewId), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(comment.getCommentId()).thenReturn(commentId);
        when(comment.getReview()).thenReturn(review);
        when(review.getReviewId()).thenReturn(reviewId);
        when(comment.getUser()).thenReturn(author);
        when(author.getUserId()).thenReturn(3L);
        when(author.getNickname()).thenReturn("영화기록자");
        when(author.getProfileImage()).thenReturn(null);
        when(comment.getContent()).thenReturn("좋은 영화였습니다.");
        when(comment.getCreatedAt()).thenReturn(createdAt);
        when(comment.getUpdatedAt()).thenReturn(createdAt);
        CommentReactionRepository.CommentReactionCountSummary counts =
                org.mockito.Mockito.mock(CommentReactionRepository.CommentReactionCountSummary.class);
        when(counts.getCommentId()).thenReturn(commentId);
        when(counts.getLikeCount()).thenReturn(2L);
        when(counts.getDislikeCount()).thenReturn(1L);
        when(commentReactionRepository.findReactionCountsByCommentIds(List.of(commentId)))
                .thenReturn(List.of(counts));

        Page<CommentResponse> result = commentService.getComments(reviewId, 0, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).getLikeCount());
        assertEquals(1L, result.getContent().get(0).getDislikeCount());
        assertNull(result.getContent().get(0).getMyReaction());

        verify(userRepository, never()).findByLoginId(anyString());
        verify(commentReactionRepository).findReactionCountsByCommentIds(List.of(commentId));
        verify(commentReactionRepository, never())
                .findMyReactionsByCommentIds(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyLong());
        verify(commentReactionRepository, never())
                .countByComment_CommentIdAndLiked(commentId, true);
        verify(commentReactionRepository, never())
                .findByUser_UserIdAndComment_CommentId(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
    }
}
