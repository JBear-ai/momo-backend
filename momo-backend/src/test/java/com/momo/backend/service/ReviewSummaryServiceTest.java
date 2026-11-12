package com.momo.backend.service;

import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReviewSummaryServiceTest {

    @Test
    void loadsCommentAndReactionCountsInTwoBatchQueries() {
        CommentRepository commentRepository = mock(CommentRepository.class);
        ReviewReactionRepository reactionRepository = mock(ReviewReactionRepository.class);

        CommentRepository.ReviewCommentCountSummary commentSummary =
                mock(CommentRepository.ReviewCommentCountSummary.class);
        ReviewReactionRepository.ReviewReactionCountSummary reactionSummary =
                mock(ReviewReactionRepository.ReviewReactionCountSummary.class);

        when(commentSummary.getReviewId()).thenReturn(11L);
        when(commentSummary.getCommentCount()).thenReturn(4L);
        when(reactionSummary.getReviewId()).thenReturn(11L);
        when(reactionSummary.getLikeCount()).thenReturn(3L);
        when(reactionSummary.getDislikeCount()).thenReturn(1L);

        when(commentRepository.countByReviewIds(List.of(11L, 12L)))
                .thenReturn(List.of(commentSummary));
        when(reactionRepository.findCountsByReviewIds(List.of(11L, 12L)))
                .thenReturn(List.of(reactionSummary));

        ReviewService service = new ReviewService(
                mock(ReviewRepository.class),
                mock(UserRepository.class),
                mock(MovieRepository.class),
                commentRepository,
                reactionRepository,
                mock(CommentReactionRepository.class)
        );

        var result = service.getReviewSummaries(List.of(11L, 12L, 11L));

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).reviewId());
        assertEquals(4L, result.get(0).commentCount());
        assertEquals(3L, result.get(0).likeCount());
        assertEquals(1L, result.get(0).dislikeCount());
        assertEquals(12L, result.get(1).reviewId());
        assertEquals(0L, result.get(1).commentCount());
        assertEquals(0L, result.get(1).likeCount());
        assertEquals(0L, result.get(1).dislikeCount());

        verify(commentRepository).countByReviewIds(List.of(11L, 12L));
        verify(reactionRepository).findCountsByReviewIds(List.of(11L, 12L));
        verifyNoMoreInteractions(commentRepository, reactionRepository);
    }

    @Test
    void rejectsMoreThanFiftyReviewIds() {
        ReviewService service = new ReviewService(
                mock(ReviewRepository.class),
                mock(UserRepository.class),
                mock(MovieRepository.class),
                mock(CommentRepository.class),
                mock(ReviewReactionRepository.class),
                mock(CommentReactionRepository.class)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getReviewSummaries(
                        java.util.stream.LongStream.rangeClosed(1, 51).boxed().toList())
        );
    }
}
