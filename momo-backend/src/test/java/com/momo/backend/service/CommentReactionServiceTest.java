package com.momo.backend.service;

import com.momo.backend.entity.Comment;
import com.momo.backend.entity.CommentReaction;
import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Optional;
import static org.mockito.Mockito.*;

class CommentReactionServiceTest {

    @Test
    void updatesExistingCommentReactionInsteadOfInsertingDuplicate() {
        CommentReactionRepository reactionRepository = mock(CommentReactionRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);
        Comment comment = mock(Comment.class);
        Review review = mock(Review.class);
        CommentReaction existing = mock(CommentReaction.class);

        when(user.getUserId()).thenReturn(9L);
        when(userRepository.findByLoginIdForUpdate("member")).thenReturn(Optional.of(user));
        when(commentRepository.findById(21L)).thenReturn(Optional.of(comment));
        when(comment.getReview()).thenReturn(review);
        when(review.getReviewId()).thenReturn(15L);
        when(reviewRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(review));
        when(commentRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(comment));
        when(reactionRepository.findByUser_UserIdAndComment_CommentId(9L, 21L))
                .thenReturn(Optional.of(existing));

        CommentReactionService service = new CommentReactionService(
                reactionRepository, commentRepository, reviewRepository, userRepository);

        service.saveReaction("member", 21L, true);

        verify(existing).updateReaction(true);
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void readsReactionCountsAndCommentExistenceWithOneQuery() {
        CommentReactionRepository reactionRepository = mock(CommentReactionRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CommentReactionRepository.CommentExistsAndReactionCountSummary summary =
                mock(CommentReactionRepository.CommentExistsAndReactionCountSummary.class);

        when(summary.getLikeCount()).thenReturn(5L);
        when(summary.getDislikeCount()).thenReturn(2L);
        when(reactionRepository.findResponseSummaryByCommentId(21L)).thenReturn(summary);

        CommentReactionService service = new CommentReactionService(
                reactionRepository, commentRepository, reviewRepository, userRepository);

        assertArrayEquals(new long[]{5L, 2L}, service.getReactionCounts(21L));
        verify(commentRepository, never()).existsById(anyLong());
        verify(reactionRepository).findResponseSummaryByCommentId(21L);
    }

    @Test
    void missingCommentIsDetectedByTheReactionSummaryQuery() {
        CommentReactionRepository reactionRepository = mock(CommentReactionRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(reactionRepository.findResponseSummaryByCommentId(21L)).thenReturn(null);

        CommentReactionService service = new CommentReactionService(
                reactionRepository, commentRepository, reviewRepository, userRepository);

        assertThrows(com.momo.backend.exception.CustomException.class,
                () -> service.getReactionCounts(21L));
        verify(commentRepository, never()).existsById(anyLong());
    }

    @Test
    void writesCommentReactionWithAtomicUpsert() {
        CommentReactionRepository reactionRepository = mock(CommentReactionRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);
        Comment comment = mock(Comment.class);
        Review review = mock(Review.class);

        when(user.getUserId()).thenReturn(9L);
        when(userRepository.findByLoginIdForUpdate("member")).thenReturn(Optional.of(user));
        when(commentRepository.findById(21L)).thenReturn(Optional.of(comment));
        when(comment.getReview()).thenReturn(review);
        when(review.getReviewId()).thenReturn(15L);
        when(reviewRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(review));
        when(commentRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(comment));

        CommentReactionService service = new CommentReactionService(
                reactionRepository, commentRepository, reviewRepository, userRepository);

        service.saveReaction("member", 21L, false);

        verify(reactionRepository).findByUser_UserIdAndComment_CommentId(9L, 21L);
        verify(reactionRepository).save(any(com.momo.backend.entity.CommentReaction.class));
    }
}
