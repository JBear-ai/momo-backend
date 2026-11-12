package com.momo.backend.service;

import com.momo.backend.dto.auth.SignupRequest;
import com.momo.backend.repository.CommentReactionRepository;
import com.momo.backend.repository.CommentRepository;
import com.momo.backend.repository.MovieRatingRepository;
import com.momo.backend.repository.FavoriteMovieRepository;
import com.momo.backend.repository.ReviewReactionRepository;
import com.momo.backend.repository.ReviewRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthServiceDuplicateSignupTest {

    @Test
    void returnsConflictWhenConcurrentSignupHitsDatabaseUniqueConstraint() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded-password");
        when(userRepository.existsByLoginId("new-member")).thenReturn(false);
        when(userRepository.existsByNickname("new-nickname")).thenReturn(false);
        when(userRepository.saveAndFlush(any())).thenThrow(
                new DataIntegrityViolationException("duplicate unique key"));

        AuthService service = new AuthService(
                userRepository,
                passwordEncoder,
                mock(ReviewRepository.class),
                mock(CommentRepository.class),
                mock(ReviewReactionRepository.class),
                mock(CommentReactionRepository.class),
                mock(FirebaseStorageService.class),
                mock(MovieRatingRepository.class),
                mock(FavoriteMovieRepository.class),
                mock(RefreshTokenService.class)
        );

        SignupRequest request = mock(SignupRequest.class);
        when(request.getLoginId()).thenReturn("new-member");
        when(request.getPassword()).thenReturn("Password1!");
        when(request.getNickname()).thenReturn("new-nickname");
        when(request.getEmail()).thenReturn("member@example.com");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.signUp(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository).saveAndFlush(any());
    }
}
