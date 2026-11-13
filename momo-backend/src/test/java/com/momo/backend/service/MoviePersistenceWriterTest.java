package com.momo.backend.service;

import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class MoviePersistenceWriterTest {

    @Test
    void insertsNewMovieAndFlushesTheUniqueMovieCodeBeforeReturning() {
        MovieRepository repository = mock(MovieRepository.class);
        when(repository.findById("20260010")).thenReturn(Optional.empty());

        MoviePersistenceWriter writer = new MoviePersistenceWriter(repository);
        writer.saveOrSupplement(new MovieSearchResponse(
                "20260010", "신규 영화", "20260110", "드라마", "감독 A"));

        verify(repository).saveAndFlush(argThat(movie ->
                "20260010".equals(movie.getMovieCode())
                        && "신규 영화".equals(movie.getTitle())
                        && "20260110".equals(movie.getReleaseDate())
                        && "드라마".equals(movie.getGenre())
                        && "감독 A".equals(movie.getDirector())
        ));
    }

    @Test
    void supplementsMissingFieldsInsteadOfReplacingExistingMovieData() {
        MovieRepository repository = mock(MovieRepository.class);
        Movie existing = new Movie(
                "20260011", "기존 영화 제목", "Existing title", "",
                "", "", "기존 감독", "", ""
        );
        when(repository.findById("20260011")).thenReturn(Optional.of(existing));

        MoviePersistenceWriter writer = new MoviePersistenceWriter(repository);
        writer.saveOrSupplement(new MovieSearchResponse(
                "20260011", "다른 제목", "20260111", "액션", "새 감독"));

        assertEquals("기존 영화 제목", existing.getTitle());
        assertEquals("Existing title", existing.getTitleEn());
        assertEquals("20260111", existing.getReleaseDate());
        assertEquals("액션", existing.getGenre());
        assertEquals("기존 감독", existing.getDirector());
        verify(repository, never()).saveAndFlush(any(Movie.class));
    }

    @Test
    void recoversOnlyTheExistingMovieAfterConcurrentInsertWonThePrimaryKeyRace() {
        MovieRepository repository = mock(MovieRepository.class);
        Movie existing = new Movie(
                "20260012", "먼저 저장된 제목", "", "",
                "", "", "", "", ""
        );
        when(repository.findById("20260012")).thenReturn(Optional.of(existing));

        MoviePersistenceWriter writer = new MoviePersistenceWriter(repository);
        writer.supplementAfterConcurrentInsert(new MovieSearchResponse(
                "20260012", "요청 제목", "20260112", "코미디", "감독 C"));

        assertEquals("먼저 저장된 제목", existing.getTitle());
        assertEquals("20260112", existing.getReleaseDate());
        assertEquals("코미디", existing.getGenre());
        verify(repository, never()).saveAndFlush(any(Movie.class));
    }

    @Test
    void usesIndependentTransactionsForInsertAndConflictRecovery() throws Exception {
        Transactional insert = MoviePersistenceWriter.class
                .getMethod("saveOrSupplement", MovieSearchResponse.class)
                .getAnnotation(Transactional.class);
        Transactional recovery = MoviePersistenceWriter.class
                .getMethod("supplementAfterConcurrentInsert", MovieSearchResponse.class)
                .getAnnotation(Transactional.class);

        assertNotNull(insert);
        assertNotNull(recovery);
        assertEquals(Propagation.REQUIRES_NEW, insert.propagation());
        assertEquals(Propagation.REQUIRES_NEW, recovery.propagation());
    }
}
