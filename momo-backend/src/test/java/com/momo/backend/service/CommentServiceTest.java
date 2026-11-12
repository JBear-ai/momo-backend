package com.momo.backend.service;

import com.momo.backend.dto.comment.CommentResponse;
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
    void anonymousVisitorCanReadCommentsWithoutPersonalReactionLookup() {
        Long reviewId = 15L;
        Long commentId = 27L;
        LocalDateTime createdAt = LocalDateTime.now();

        Comment comment = org.mockito.Mockito.mock(Comment.class);
        Review review = org.mockito.Mockito.mock(Review.class);
        User author = org.mockito.Mockito.mock(User.class);

        when(reviewRepository.existsById(reviewId)).thenReturn(true);
        when(commentRepository.findByReview_ReviewIdOrderByCreatedAtAsc(
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
        CommentReactionRepository.CommentReactionSummary firstLike =
                org.mockito.Mockito.mock(CommentReactionRepository.CommentReactionSummary.class);
        CommentReactionRepository.CommentReactionSummary secondLike =
                org.mockito.Mockito.mock(CommentReactionRepository.CommentReactionSummary.class);
        CommentReactionRepository.CommentReactionSummary dislike =
                org.mockito.Mockito.mock(CommentReactionRepository.CommentReactionSummary.class);

        when(firstLike.getCommentId()).thenReturn(commentId);
        when(firstLike.getLiked()).thenReturn(true);
        when(firstLike.getUserId()).thenReturn(10L);
        when(secondLike.getCommentId()).thenReturn(commentId);
        when(secondLike.getLiked()).thenReturn(true);
        when(secondLike.getUserId()).thenReturn(11L);
        when(dislike.getCommentId()).thenReturn(commentId);
        when(dislike.getLiked()).thenReturn(false);
        when(dislike.getUserId()).thenReturn(12L);
        when(commentReactionRepository.findReactionSummariesByCommentIds(List.of(commentId)))
                .thenReturn(List.of(firstLike, secondLike, dislike));

        Page<CommentResponse> result = commentService.getComments(reviewId, 0, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).getLikeCount());
        assertEquals(1L, result.getContent().get(0).getDislikeCount());
        assertNull(result.getContent().get(0).getMyReaction());

        verify(userRepository, never()).findByLoginId(anyString());
        verify(commentReactionRepository).findReactionSummariesByCommentIds(List.of(commentId));
        verify(commentReactionRepository, never())
                .countByComment_CommentIdAndLiked(commentId, true);
        verify(commentReactionRepository, never())
                .findByUser_UserIdAndComment_CommentId(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
    }
}
