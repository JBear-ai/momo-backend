package com.momo.backend.service;

import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Optional;
import static org.mockito.Mockito.*;

class ReviewReactionServiceTest {

    @Test
    void updatesExistingReviewReactionInsteadOfInsertingDuplicate() {
        ReviewReactionRepository reactionRepository = mock(ReviewReactionRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);
        com.momo.backend.entity.ReviewReactions existing =
                mock(com.momo.backend.entity.ReviewReactions.class);

        when(user.getUserId()).thenReturn(7L);
        when(userRepository.findByLoginIdForUpdate("member")).thenReturn(Optional.of(user));
        when(reviewRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(mock(Review.class)));
        when(reactionRepository.findByUser_UserIdAndReview_ReviewId(7L, 11L))
                .thenReturn(Optional.of(existing));

        ReviewReactionService service = new ReviewReactionService(
                reactionRepository, reviewRepository, userRepository);

        service.saveReactions("member", 11L, false);

        verify(existing).updateReaction(false);
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void readsReactionCountsAndReviewExistenceWithOneQuery() {
        ReviewReactionRepository reactionRepository = mock(ReviewReactionRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ReviewReactionRepository.ReviewResponseSummary summary =
                mock(ReviewReactionRepository.ReviewResponseSummary.class);

        when(summary.getLikeCount()).thenReturn(4L);
        when(summary.getDislikeCount()).thenReturn(1L);
        when(reactionRepository.findResponseSummaryByReviewId(11L)).thenReturn(summary);

        ReviewReactionService service = new ReviewReactionService(
                reactionRepository, reviewRepository, userRepository);

        assertArrayEquals(new long[]{4L, 1L}, service.getReactionCounts(11L));
        verify(reviewRepository, never()).existsById(anyLong());
        verify(reactionRepository).findResponseSummaryByReviewId(11L);
    }

    @Test
    void missingReviewIsDetectedByTheReactionSummaryQuery() {
        ReviewReactionRepository reactionRepository = mock(ReviewReactionRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(reactionRepository.findResponseSummaryByReviewId(11L)).thenReturn(null);

        ReviewReactionService service = new ReviewReactionService(
                reactionRepository, reviewRepository, userRepository);

        assertThrows(com.momo.backend.exception.CustomException.class,
                () -> service.getReactionCounts(11L));
        verify(reviewRepository, never()).existsById(anyLong());
    }

    @Test
    void writesReviewReactionWithAtomicUpsert() {
        ReviewReactionRepository reactionRepository = mock(ReviewReactionRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);

        when(user.getUserId()).thenReturn(7L);
        when(userRepository.findByLoginIdForUpdate("member")).thenReturn(Optional.of(user));
        when(reviewRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(mock(Review.class)));

        ReviewReactionService service = new ReviewReactionService(
                reactionRepository, reviewRepository, userRepository);

        service.saveReactions("member", 11L, true);

        verify(reactionRepository).findByUser_UserIdAndReview_ReviewId(7L, 11L);
        verify(reactionRepository).save(any(com.momo.backend.entity.ReviewReactions.class));
    }
}
