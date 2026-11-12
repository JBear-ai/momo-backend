package com.momo.backend.service;

import com.momo.backend.entity.User;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.MovieRatingRepository;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.Mockito.*;

class AuthServiceAccountDeletionTest {

    @Test
    void deletesMemberDataWithBulkQueriesBeforeDeletingTheUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        ReviewReactionRepository reviewReactionRepository =
                mock(ReviewReactionRepository.class);
        CommentReactionRepository commentReactionRepository =
                mock(CommentReactionRepository.class);
        MovieRatingRepository movieRatingRepository = mock(MovieRatingRepository.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

        User user = mock(User.class);
        when(userRepository.findByLoginId("member")).thenReturn(java.util.Optional.of(user));
        when(user.getUserId()).thenReturn(41L);
        when(user.getPasswordHash()).thenReturn("encoded-password");
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);

        AuthService service = new AuthService(
                userRepository,
                passwordEncoder,
                reviewRepository,
                commentRepository,
                reviewReactionRepository,
                commentReactionRepository,
                mock(FirebaseStorageService.class),
                movieRatingRepository,
                refreshTokenService
        );

        service.deleteMyAccount("member", "current-password");

        InOrder order = inOrder(
                refreshTokenService,
                commentReactionRepository,
                reviewReactionRepository,
                commentRepository,
                movieRatingRepository,
                userRepository
        );
        order.verify(refreshTokenService).deleteByUserId(41L);
        order.verify(commentReactionRepository).deleteAuthoredByUserId(41L);
        order.verify(commentReactionRepository).deleteForCommentsAuthoredByUser(41L);
        order.verify(commentRepository).deleteAuthoredByUserId(41L);
        order.verify(commentReactionRepository).deleteForReviewsAuthoredByUser(41L);
        order.verify(reviewReactionRepository).deleteAuthoredByUserId(41L);
        order.verify(reviewReactionRepository).deleteForReviewsAuthoredByUser(41L);
        order.verify(commentRepository).deleteForReviewsAuthoredByUser(41L);
        order.verify(reviewRepository).deleteAuthoredByUserId(41L);
        order.verify(movieRatingRepository).deleteAuthoredByUserId(41L);
        order.verify(userRepository).delete(user);

        verify(commentReactionRepository, never()).deleteByUser_UserId(anyLong());
        verify(commentReactionRepository, never()).deleteByComment_User_UserId(anyLong());
        verify(reviewReactionRepository, never()).deleteByUser_UserId(anyLong());
        verify(movieRatingRepository, never()).deleteByUser_UserId(anyLong());
    }
}
