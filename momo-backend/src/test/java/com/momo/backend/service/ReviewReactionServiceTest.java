package com.momo.backend.service;

import com.momo.backend.entity.Review;
import com.momo.backend.entity.User;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
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
    void writesReviewReactionWithAtomicUpsert() {
        ReviewReactionRepository reactionRepository = mock(ReviewReactionRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);

        when(user.getUserId()).thenReturn(7L);
        when(userRepository.findByLoginIdForUpdate("member")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(11L)).thenReturn(Optional.of(mock(Review.class)));

        ReviewReactionService service = new ReviewReactionService(
                reactionRepository, reviewRepository, userRepository);

        service.saveReactions("member", 11L, true);

        verify(reactionRepository).findByUser_UserIdAndReview_ReviewId(7L, 11L);
        verify(reactionRepository).save(any(com.momo.backend.entity.ReviewReactions.class));
    }
}
