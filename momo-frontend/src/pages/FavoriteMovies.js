import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getFavoriteMovies } from "../utils/favorites";
import FavoriteStar from "../components/common/FavoriteStar";
import { resolvePosterUrl } from "../utils/mediaUrl";

function FavoriteMovies() {
  const navigate = useNavigate();
  const [movies, setMovies] = useState(() => getFavoriteMovies());

  // 관심 영화 목록 갱신
  useEffect(() => {
    const updateFavorites = (event) => {
      setMovies(event.detail || getFavoriteMovies());
    };

    window.addEventListener("momo-favorites-changed", updateFavorites);

    return () => {
      window.removeEventListener("momo-favorites-changed", updateFavorites);
    };
  }, []);

  return (
    React.createElement(
  "main",
  {"className":"favorite-page"},
  React.createElement(
      "div",
      {"className":"favorite-page-container"},
      React.createElement(
            "header",
            {"className":"favorite-page-heading"},
            React.createElement(
                    "h1",
                    null,
                    "관심 영화"
                  ),
            React.createElement(
                    "p",
                    null,
                    "마음에 드는 영화를 모아 나만의 영화 목록을 만들어 보세요."
                  )
          ),
      React.createElement(
            "div",
            {"className":"favorite-list-toolbar"},
            React.createElement(
                    "span",
                    null,
                    "총 ",
                    (movies.length),
                    "개의 영화"
                  ),
            React.createElement(
                    "button",
                    {"type":"button","className":"favorite-back-button","onClick":() => navigate("/profile")},
                    "← 마이페이지"
                  )
          ),
      (movies.length === 0 ? (
                React.createElement(
            "div",
            {"className":"favorite-page-empty"},
            React.createElement(
                    "span",
                    {"className":"favorite-empty-symbol"},
                    "🎬"
                  ),
            React.createElement(
                    "h2",
                    null,
                    "아직 등록한 관심 영화가 없습니다."
                  ),
            React.createElement(
                    "p",
                    null,
                    "마음에 드는 영화를 관심 영화로 등록해 보세요."
                  ),
            React.createElement(
                    "button",
                    {"type":"button","className":"favorite-browse-button","onClick":() => navigate("/")},
                    "영화 둘러보기"
                  )
          )
              ) : (
                React.createElement(
            "div",
            {"className":"favorite-page-grid"},
            (movies.map((movie) => (
                          React.createElement(
                    "article",
                    {"className":"favorite-page-card","key":movie.movieCode},
                    React.createElement(
                              "div",
                              {"className":"favorite-page-poster"},
                              React.createElement(
                                          "button",
                                          {"type":"button","onClick":() => navigate(`/movie/${movie.movieCode}`),"aria-label":`${movie.title} 상세 보기`},
                                          (movie.posterUrl ? (
                                                                React.createElement(
                                                        "img",
                                                        {"src":resolvePosterUrl(movie.posterUrl),"alt":`${movie.title} 포스터`,"onError":(event) => {
                                                                                  event.currentTarget.style.display = "none";
                                                                                }}
                                                      )
                                                              ) : (
                                                                React.createElement(
                                                        "div",
                                                        {"className":"favorite-page-no-poster"},
                                                        "포스터 없음"
                                                      )
                                                              ))
                                        ),
                              React.createElement(
                                          FavoriteStar,
                                          {"movie":movie}
                                        )
                            ),
                    React.createElement(
                              "div",
                              {"className":"favorite-page-movie-info"},
                              React.createElement(
                                          "h3",
                                          null,
                                          (movie.title)
                                        ),
                              React.createElement(
                                          "p",
                                          null,
                                          (movie.releaseDate || "개봉일 정보 없음")
                                        )
                            )
                  )
                        )))
          )
              ))
    )
)
  );
}

export default FavoriteMovies;
