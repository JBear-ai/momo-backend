package com.momo.backend.service;

import com.momo.backend.entity.Movie;
import com.momo.backend.entity.MovieRating;
import com.momo.backend.entity.User;
import com.momo.backend.repository.MovieRatingRepository;
import com.momo.backend.repository.MovieRepository;
import com.momo.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                ratingRepository, movieRepository, userRepository, mock(MovieService.class));

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
                ratingRepository, movieRepository, userRepository, mock(MovieService.class));

        service.saveRating("member", "20261234", false);

        verify(userRepository).findByLoginIdForUpdate("member");
        verify(existing).updateRating(false);
        verify(ratingRepository, never()).save(any(MovieRating.class));
    }

    @Test
    void readsCountsAndCurrentMemberReactionWithOneAggregateQuery() {
        UserRepository userRepository = mock(UserRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieRatingRepository ratingRepository = mock(MovieRatingRepository.class);
        MovieRatingRepository.MovieRatingResponseSummary summary =
                mock(MovieRatingRepository.MovieRatingResponseSummary.class);

        User user = mock(User.class);
        MovieRating rating = mock(MovieRating.class);
        when(movieRepository.existsById("20261234")).thenReturn(true);
        when(ratingRepository.countByMovie_MovieCodeAndLiked("20261234", true)).thenReturn(8L);
        when(ratingRepository.countByMovie_MovieCodeAndLiked("20261234", false)).thenReturn(2L);
        when(userRepository.findByLoginId("member")).thenReturn(Optional.of(user));
        when(user.getUserId()).thenReturn(7L);
        when(ratingRepository.findByUser_UserIdAndMovie_MovieCode(7L, "20261234"))
                .thenReturn(Optional.of(rating));
        when(rating.getLiked()).thenReturn(true);

        MovieRatingService service = new MovieRatingService(
                ratingRepository, movieRepository, userRepository, mock(MovieService.class));

        Map<String, Object> result = service.getMovieRating("20261234", "member");

        assertEquals(8L, result.get("likeCount"));
        assertEquals(2L, result.get("dislikeCount"));
        assertEquals(10L, result.get("totalCount"));
        assertEquals(80.0, result.get("positiveRate"));
        assertEquals(true, result.get("myRating"));

        verify(movieRepository).existsById("20261234");
        verify(ratingRepository).countByMovie_MovieCodeAndLiked("20261234", true);
        verify(ratingRepository).countByMovie_MovieCodeAndLiked("20261234", false);
        verify(ratingRepository).findByUser_UserIdAndMovie_MovieCode(7L, "20261234");
    }

    @Test
    void registersMissingMovieBeforeReturningAnEmptyRatingSummary() {
        UserRepository userRepository = mock(UserRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieRatingRepository ratingRepository = mock(MovieRatingRepository.class);
        MovieService movieService = mock(MovieService.class);

        when(movieRepository.existsById("20167904"))
                .thenReturn(false, true);
        MovieRatingService service = new MovieRatingService(
                ratingRepository, movieRepository, userRepository, movieService);

        Map<String, Object> result = service.getMovieRating("20167904", null);

        verify(movieService).getMovieDetail("20167904");
        assertEquals(0L, result.get("likeCount"));
        assertEquals(0L, result.get("dislikeCount"));
        assertEquals(0L, result.get("totalCount"));
        assertNull(result.get("myRating"));
    }

    @Test
    void anonymousMovieRatingDoesNotPerformMemberLookup() {
        UserRepository userRepository = mock(UserRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieRatingRepository ratingRepository = mock(MovieRatingRepository.class);
        MovieRatingRepository.MovieRatingResponseSummary summary =
                mock(MovieRatingRepository.MovieRatingResponseSummary.class);

        when(movieRepository.existsById("20261234")).thenReturn(true);
        when(ratingRepository.countByMovie_MovieCodeAndLiked("20261234", true)).thenReturn(0L);
        when(ratingRepository.countByMovie_MovieCodeAndLiked("20261234", false)).thenReturn(0L);

        MovieRatingService service = new MovieRatingService(
                ratingRepository, movieRepository, userRepository, mock(MovieService.class));

        Map<String, Object> result = service.getMovieRating("20261234", null);

        assertEquals(0L, result.get("likeCount"));
        assertEquals(0L, result.get("dislikeCount"));
        assertEquals(0L, result.get("totalCount"));
        assertNull(result.get("positiveRate"));
        assertNull(result.get("myRating"));

        verify(movieRepository).existsById("20261234");
        verify(ratingRepository).countByMovie_MovieCodeAndLiked("20261234", true);
        verify(ratingRepository).countByMovie_MovieCodeAndLiked("20261234", false);
        verify(userRepository, never()).findByLoginId(anyString());
    }
}
