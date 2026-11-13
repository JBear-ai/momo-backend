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

    @Test
    void readsCountsAndCurrentMemberReactionWithOneAggregateQuery() {
        UserRepository userRepository = mock(UserRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieRatingRepository ratingRepository = mock(MovieRatingRepository.class);
        MovieRatingRepository.MovieRatingResponseSummary summary =
                mock(MovieRatingRepository.MovieRatingResponseSummary.class);

        when(summary.getLikeCount()).thenReturn(8L);
        when(summary.getDislikeCount()).thenReturn(2L);
        when(summary.getMyRating()).thenReturn(1);
        when(ratingRepository.findResponseSummaryByMovieCode("20261234", "member"))
                .thenReturn(summary);

        MovieRatingService service = new MovieRatingService(
                ratingRepository, movieRepository, userRepository);

        Map<String, Object> result = service.getMovieRating("20261234", "member");

        assertEquals(8L, result.get("likeCount"));
        assertEquals(2L, result.get("dislikeCount"));
        assertEquals(10L, result.get("totalCount"));
        assertEquals(80.0, result.get("positiveRate"));
        assertEquals(true, result.get("myRating"));

        verify(ratingRepository).findResponseSummaryByMovieCode("20261234", "member");
        verify(movieRepository, never()).existsById(anyString());
        verify(ratingRepository, never()).findCountsByMovieCode(anyString());
        verify(ratingRepository, never())
                .findByUser_UserIdAndMovie_MovieCode(anyLong(), anyString());
        verify(userRepository, never()).findByLoginId(anyString());
    }

    @Test
    void missingMovieIsDetectedByTheAggregateQueryWithoutAnExtraExistsQuery() {
        UserRepository userRepository = mock(UserRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieRatingRepository ratingRepository = mock(MovieRatingRepository.class);

        when(ratingRepository.findResponseSummaryByMovieCode("missing", null))
                .thenReturn(null);

        MovieRatingService service = new MovieRatingService(
                ratingRepository, movieRepository, userRepository);

        assertThrows(com.momo.backend.exception.CustomException.class,
                () -> service.getMovieRating("missing", null));

        verify(movieRepository, never()).existsById(anyString());
        verify(ratingRepository).findResponseSummaryByMovieCode("missing", null);
    }

    @Test
    void anonymousMovieRatingDoesNotPerformMemberLookup() {
        UserRepository userRepository = mock(UserRepository.class);
        MovieRepository movieRepository = mock(MovieRepository.class);
        MovieRatingRepository ratingRepository = mock(MovieRatingRepository.class);
        MovieRatingRepository.MovieRatingResponseSummary summary =
                mock(MovieRatingRepository.MovieRatingResponseSummary.class);

        when(movieRepository.existsById("20261234")).thenReturn(true);
        when(summary.getLikeCount()).thenReturn(0L);
        when(summary.getDislikeCount()).thenReturn(0L);
        when(summary.getMyRating()).thenReturn(null);
        when(ratingRepository.findResponseSummaryByMovieCode("20261234", null))
                .thenReturn(summary);

        MovieRatingService service = new MovieRatingService(
                ratingRepository, movieRepository, userRepository);

        Map<String, Object> result = service.getMovieRating("20261234", null);

        assertEquals(0L, result.get("likeCount"));
        assertEquals(0L, result.get("dislikeCount"));
        assertEquals(0L, result.get("totalCount"));
        assertNull(result.get("positiveRate"));
        assertNull(result.get("myRating"));

        verify(ratingRepository).findResponseSummaryByMovieCode("20261234", null);
        verify(movieRepository, never()).existsById(anyString());
        verify(userRepository, never()).findByLoginId(anyString());
    }
}
