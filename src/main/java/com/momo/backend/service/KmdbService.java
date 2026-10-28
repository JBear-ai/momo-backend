
package com.momo.backend.service;

import com.momo.backend.client.KmdbApiClient;
import com.momo.backend.dto.movie.MovieDetailResponse;
import com.momo.backend.dto.movie.MoviePosterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class KmdbService {

    private final KmdbApiClient kmdbApiClient;
    private final MovieService movieService;
    private final ObjectMapper objectMapper;

    // KOBIS 영화 코드로 KMDb 포스터 검색
    public MoviePosterResponse getMoviePoster(String movieCode) {

        MovieDetailResponse movie = movieService.getMovieDetail(movieCode);

        String title = movie.title();
        String openDate = movie.releaseDate().replace("-", "");

        String json = kmdbApiClient.searchMovie(title);

        try {
            JsonNode dataList = objectMapper.readTree(json).path("Data");

            for (JsonNode data : dataList) {
                for (JsonNode result : data.path("Result")) {

                    String kmdbTitle = result.path("title")
                            .asText("").replaceAll("!HS|!HE", "").trim();

                    String releaseDate = result.path("repRlsDate")
                            .asText("");

                    String posters = result.path("posters").asText("");

                    // 제목이 다른 영화는 제외
                    if (!kmdbTitle.replaceAll("\\s+", "")
                            .equalsIgnoreCase(title.replaceAll("\\s+", ""))) {
                        continue;
                    }

                    // 개봉일이 둘 다 존재하면 비교
                    if (!openDate.isBlank() && !releaseDate.isBlank()
                            && !openDate.equals(releaseDate)) {
                        continue;
                    }

                    if (!posters.isBlank()) {
                        String posterUrl = posters.split("\\|")[0].trim();

                        return new MoviePosterResponse(movieCode, posterUrl);
                    }
                }
            }

            // 일치하는 포스터가 없는 경우
            return new MoviePosterResponse(movieCode, null);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "KMDb 포스터 검색 결과를 처리하지 못했습니다.", e);
        }
    }
}
