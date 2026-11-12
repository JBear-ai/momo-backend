package com.momo.backend.service;

import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MovieRating;
import com.momo.backend.entity.User;
import com.momo.backend.repository.MovieRatingRepository;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class MovieRatingServiceTest {

    @Test
    void locksMemberBeforeCreatingMovieRating() {
        UserRepository userRepository = mock(UserRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieRatingRepository ratingRepository = mock(MovieRatingRepository.class);
        User user = mock(User.class);
        Movie movie = mock(Movie.class);

        when(user.getUserId()).thenReturn(7L);
        when(userRepository.findByLoginIdForUpdate("member"))
                .thenReturn(Optional.of(user));
        when(movieRepository.findById("20261234"))
                .thenReturn(Optional.of(movie));
        when(ratingRepository.findByUser_UserIdAndMovie_MovieCode(7L, "20261234"))
                .thenReturn(Optional.empty());

        MovieRatingService service = new MovieRatingService(
                ratingRepository, movieRepository, userRepository);

        service.saveRating("member", "20261234", true);

        verify(userRepository).findByLoginIdForUpdate("member");
        verify(userRepository, never()).findByLoginId("member");
        verify(ratingRepository).save(any(MovieRating.class));
    }

    @Test
    void locksMemberBeforeUpdatingExistingMovieRating() {
        UserRepository userRepository = mock(UserRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieRatingRepository ratingRepository = mock(MovieRatingRepository.class);
        User user = mock(User.class);
        Movie movie = mock(Movie.class);
        MovieRating existing = mock(MovieRating.class);

        when(user.getUserId()).thenReturn(7L);
        when(userRepository.findByLoginIdForUpdate("member"))
                .thenReturn(Optional.of(user));
        when(movieRepository.findById("20261234"))
                .thenReturn(Optional.of(movie));
        when(ratingRepository.findByUser_UserIdAndMovie_MovieCode(7L, "20261234"))
                .thenReturn(Optional.of(existing));

        MovieRatingService service = new MovieRatingService(
                ratingRepository, movieRepository, userRepository);

        service.saveRating("member", "20261234", false);

        verify(userRepository).findByLoginIdForUpdate("member");
        verify(existing).updateRating(false);
        verify(ratingRepository, never()).save(any(MovieRating.class));
    }
}
