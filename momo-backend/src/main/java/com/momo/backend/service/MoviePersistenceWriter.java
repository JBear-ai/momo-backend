package com.momo.backend.service;

import com.momo.backend.dto.movie.MovieSearchResponse;
import com.momo.backend.entity.Movie;
import com.momo.backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영화 한 건의 저장을 독립 트랜잭션으로 처리한다.
 * 같은 영화 코드가 다른 요청에서 동시에 INSERT되면 PK 충돌을 호출자에게 돌려보내고,
 * 호출자는 롤백이 끝난 뒤 별도 트랜잭션에서 기존 행의 누락 정보만 보완한다.
 */
@Service
@RequiredArgsConstructor
public class MoviePersistenceWriter {

    private final MovieRepository movieRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOrSupplement(MovieSearchResponse item) {
        Movie existing = movieRepository.findById(item.movieCode()).orElse(null);
        if (existing != null) {
            supplement(existing, item);
            return;
        }

        Movie newMovie = new Movie(
                item.movieCode(),
                item.title(),
                "",
                item.releaseDate(),
                "",
                item.genre(),
                item.director(),
                "",
                ""
        );

        // 반드시 현재 트랜잭션 안에서 INSERT를 확정해 중복 키 충돌 지점을 명확히 한다.
        movieRepository.saveAndFlush(newMovie);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void supplementAfterConcurrentInsert(MovieSearchResponse item) {
        Movie existing = movieRepository.findById(item.movieCode())
                .orElseThrow(() -> new IllegalStateException(
                        "동시 저장 충돌 이후 영화 데이터가 존재하지 않습니다. movieCode="
                                + item.movieCode()
                ));
        supplement(existing, item);
    }

    private void supplement(Movie movie, MovieSearchResponse item) {
        movie.supplementMissingDetails(
                "",
                item.releaseDate(),
                "",
                item.genre(),
                item.director(),
                "",
                "",
                "KOBIS_SEARCH"
        );
    }
}
