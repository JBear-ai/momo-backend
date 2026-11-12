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
    void writesReviewReactionWithAtomicUpsert() {
        ReviewReactionRepository reactionRepository = mock(ReviewReactionRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);

        when(user.getUserId()).thenReturn(7L);
        when(userRepository.findByLoginId("member")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(11L)).thenReturn(Optional.of(mock(Review.class)));

        ReviewReactionService service = new ReviewReactionService(
                reactionRepository, reviewRepository, userRepository);

        service.saveReactions("member", 11L, true);

        verify(reactionRepository).upsertReaction(7L, 11L, true);
        verify(reactionRepository, never())
                .findByUser_UserIdAndReview_ReviewId(anyLong(), anyLong());
    }
}
