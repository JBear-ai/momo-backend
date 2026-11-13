package com.momo.backend.service;

import com.momo.backend.dto.review.ReviewCreateRequest;
import com.momo.backend.entity.User;
import com.momo.backend.exception.CustomException;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReviewServiceDuplicateCreateTest {

    @Test
    void returnsDuplicateReviewConflictWhenConcurrentInsertHitsUniqueConstraint() {
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(5L);
        when(userRepository.findByLoginIdForUpdate("member")).thenReturn(Optional.of(user));
        when(reviewRepository.existsByUser_UserIdAndMovieCode(5L, "20260001"))
                .thenReturn(false);
        when(reviewRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate user/movie"));

        ReviewService service = new ReviewService(
                reviewRepository,
                userRepository,
                mock(MovieRepository.class),
                mock(CommentRepository.class),
                mock(ReviewReactionRepository.class),
                mock(CommentReactionRepository.class)
        );

        ReviewCreateRequest request = mock(ReviewCreateRequest.class);
        when(request.getMovieCode()).thenReturn("20260001");
        when(request.getTitle()).thenReturn("리뷰 제목");
        when(request.getContent()).thenReturn("리뷰 내용");

        assertThrows(
                CustomException.class,
                () -> service.createReview("member", request)
        );
        verify(reviewRepository).saveAndFlush(any());
    }
}
