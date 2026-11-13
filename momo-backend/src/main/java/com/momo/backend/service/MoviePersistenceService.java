package com.momo.backend.service;

import com.momo.backend.dto.movie.MovieSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePersistenceService {

    private final MoviePersistenceWriter moviePersistenceWriter;

    // 검색 결과에 포함된 영화를 코드 기준으로 중복 제거한 뒤 한 건씩 안전하게 저장한다.
    // 각 저장은 독립 트랜잭션이므로 동시 INSERT 충돌이 전체 검색 결과를 롤백시키지 않는다.
    public void saveSearchMovies(List<MovieSearchResponse> movies) {
        if (movies == null || movies.isEmpty()) {
            return;
        }

        Map<String, MovieSearchResponse> uniqueMovies = new LinkedHashMap<>();
        for (MovieSearchResponse item : movies) {
            if (item == null
                    || item.movieCode() == null
                    || item.movieCode().isBlank()
                    || item.title() == null
                    || item.title().isBlank()) {
                continue;
            }

            MovieSearchResponse previous = uniqueMovies.putIfAbsent(item.movieCode(), item);
            if (previous != null) {
                log.debug("중복 영화 검색 결과 제거. movieCode={}", item.movieCode());
            }
        }

        int savedOrUpdated = 0;
        int recoveredConflicts = 0;

        for (MovieSearchResponse item : uniqueMovies.values()) {
            try {
                moviePersistenceWriter.saveOrSupplement(item);
                savedOrUpdated++;
            } catch (DataIntegrityViolationException duplicate) {
                // 다른 요청이 같은 영화 코드를 먼저 저장한 경우,
                // 실패한 트랜잭션이 롤백된 뒤 기존 행만 보완한다.
                moviePersistenceWriter.supplementAfterConcurrentInsert(item);
                recoveredConflicts++;
            }
        }

        log.info(
                "검색 영화 DB 저장 완료. 처리={}, 동시 저장 충돌 복구={}",
                savedOrUpdated + recoveredConflicts,
                recoveredConflicts
        );
    }
}
