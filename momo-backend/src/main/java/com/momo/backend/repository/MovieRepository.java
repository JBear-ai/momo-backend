package com.momo.backend.repository;

import com.momo.backend.entity.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, String> {

    // 무결성 스케줄러가 검사한 파일 경로와 현재 DB 경로가 같을 때만 제거한다.
    // 검사와 복구 사이에 새 포스터가 저장된 경우 최신 경로를 덮어쓰지 않는다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update Movie movie
               set movie.posterUrl = null
             where movie.movieCode = :movieCode
               and movie.posterUrl = :expectedPosterUrl
            """)
    int clearPosterUrlIfMatches(
            @Param("movieCode") String movieCode,
            @Param("expectedPosterUrl") String expectedPosterUrl
    );


    List<Movie> findByBoxOfficeEnteredTrueOrderByBestBoxOfficeRankAscLastBoxOfficeDateDesc();

    // 로컬 포스터 무결성 점검은 영화 코드 커서 방식으로 조회해 누락 행이 제거되어도 건너뛰지 않는다.
    List<Movie> findTop100ByPosterUrlStartingWithOrderByMovieCodeAsc(String posterUrlPrefix);

    List<Movie> findTop100ByPosterUrlStartingWithAndMovieCodeGreaterThanOrderByMovieCodeAsc(
            String posterUrlPrefix,
            String afterMovieCode
    );


    Page<Movie> findByGenreContainingIgnoreCaseOrderByReleaseDateDescMovieCodeAsc(String genre, Pageable pageable);

    List<Movie> findByBoxOfficeEnteredTrueAndGenreContainingIgnoreCaseOrderByBestBoxOfficeRankAscLastBoxOfficeDateDesc(
            String genre
    );

    @Query("""
            select movie
              from Movie movie
             where movie.posterUrl like 'http%'
               and (movie.posterMigrationAttemptedAt is null
                 or movie.posterMigrationAttemptedAt < :retryBefore)
             order by movie.movieCode asc
            """)
    List<Movie> findMoviesNeedingPosterMigration(
            @Param("retryBefore") LocalDateTime retryBefore,
            Pageable pageable
    );

    @Query("""
            select movie
              from Movie movie
             where (movie.titleEn is null or trim(movie.titleEn) = ''
                 or movie.releaseDate is null or trim(movie.releaseDate) = ''
                 or movie.showTime is null or trim(movie.showTime) = ''
                 or movie.genre is null or trim(movie.genre) = ''
                 or movie.director is null or trim(movie.director) = ''
                 or movie.actors is null or trim(movie.actors) = ''
                 or movie.watchGrade is null or trim(movie.watchGrade) = '')
               and (movie.kobisDetailAttemptedAt is null
                 or movie.kobisDetailAttemptedAt < :retryBefore)
             order by movie.movieCode asc
            """)
    List<Movie> findMoviesNeedingMetadata(
            @Param("retryBefore") LocalDateTime retryBefore,
            Pageable pageable
    );
}
