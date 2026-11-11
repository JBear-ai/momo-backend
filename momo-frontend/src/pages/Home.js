import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import TodayRecommend from "../components/common/TodayRecommend";
import FavoriteStar from "../components/common/FavoriteStar";
import { getFavoriteMovies } from "../utils/favorites";
import { resolvePosterUrl } from "../utils/mediaUrl";

import {
  HomePage,
  FeatureSection,
  FeatureInfo,
  Eyebrow,
  FeatureTitle,
  FeatureDescription,
  FeatureVisual,
  Section,
  SectionHeading,
  MoreText,
  MovieGrid,
  MovieCard,
  PosterWrap,
  Rank,
  MovieInfo,
  GenreList,
  BrandLine,
  EmptyPoster,
} from "./Home.styles";

// 어제 날짜를 YYYYMMDD 형식으로 반환
const getYesterdayDate = () => {
  const date = new Date();
  date.setDate(date.getDate() - 1);

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}${month}${day}`;
};

function Home() {
  const navigate = useNavigate();

  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [favoriteMovies, setFavoriteMovies] = useState(() =>
    getFavoriteMovies(),
  );

  // 관심 영화 목록이 바뀌면 화면을 갱신
  useEffect(() => {
    const updateFavorites = (event) => {
      setFavoriteMovies(event.detail || getFavoriteMovies());
    };

    window.addEventListener("momo-favorites-changed", updateFavorites);

    return () => {
      window.removeEventListener("momo-favorites-changed", updateFavorites);
    };
  }, []);

  // 어제 날짜의 박스오피스 목록 조회
  useEffect(() => {
    const targetDate = getYesterdayDate();

    fetch("http://localhost:8111/api/boxoffice/daily?targetDate=" + targetDate)
      .then((response) => {
        if (!response.ok) {
          throw new Error("박스오피스 조회 실패");
        }

        return response.json();
      })
      .then((data) => {
        const list = Array.isArray(data)
          ? data
          : data.movies || data.dailyBoxOfficeList || data.boxOfficeList || [];

        const normalized = list.map((movie, index) => ({
          movieCode: movie.movieCode || movie.movieCd || movie.id,
          title: movie.title || movie.movieNm || movie.movieName,
          releaseDate: movie.releaseDate || movie.openDt || "",
          posterUrl:
            movie.posterUrl ||
            movie.posterPath ||
            movie.poster ||
            movie.imageUrl,
          rank: movie.rank || index + 1,
        }));

        setMovies(normalized);
      })
      .catch((error) => {
        console.error(error);
        setMovies([]);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  // 영화 상세 페이지로 이동
  const goToMovie = (movieCode) => {
    if (!movieCode) return;
    navigate("/movie/" + movieCode);
  };

  // Enter 또는 Space 키로 영화 카드 선택
  const handleMovieKeyDown = (event, movieCode) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      goToMovie(movieCode);
    }
  };

  const genres = [
    "드라마",
    "액션",
    "코미디",
    "스릴러",
    "로맨스",
    "공포",
    "애니메이션",
    "다큐멘터리",
  ];

  return (
    React.createElement(
  HomePage,
  null,
  React.createElement(
      FeatureSection,
      null,
      React.createElement(
            FeatureInfo,
            null,
            React.createElement(
                    Eyebrow,
                    null,
                    "MOMO · 오늘의 영화"
                  ),
            React.createElement(
                    FeatureTitle,
                    null,
                    "오늘은 어떤 영화를"
                  ),
            React.createElement(
                    FeatureTitle,
                    null,
                    "기억하고 싶나요?"
                  ),
            React.createElement(
                    FeatureDescription,
                    null,
                    "영화를 보고 떠오른 생각을 기록하고,",
                    React.createElement(
                              "br",
                              null
                            ),
                    "다른 사람의 감상도 함께 살펴보세요."
                  )
          ),
      React.createElement(
            FeatureVisual,
            null,
            React.createElement(
                    TodayRecommend,
                    null
                  )
          )
    ),
  React.createElement(
      Section,
      null,
      React.createElement(
            SectionHeading,
            null,
            React.createElement(
                    "div",
                    null,
                    React.createElement(
                              "h2",
                              null,
                              "오늘의 박스오피스"
                            ),
                    React.createElement(
                              "p",
                              null,
                              "가장 많은 관객이 선택한 영화"
                            )
                  ),
            React.createElement(
                    MoreText,
                    null,
                    "일일 관객 순위"
                  )
          ),
      (loading ? (
                React.createElement(
            "p",
            null,
            "영화 정보를 불러오는 중입니다."
          )
              ) : movies.length === 0 ? (
                React.createElement(
            "p",
            null,
            "현재 표시할 영화가 없습니다."
          )
              ) : (
                React.createElement(
            MovieGrid,
            null,
            (movies.slice(0, 10).map((movie, index) => (
                          React.createElement(
                    MovieCard,
                    {"key":movie.movieCode || index,"role":"button","tabIndex":0,"onClick":() => goToMovie(movie.movieCode),"onKeyDown":(event) =>
                                      handleMovieKeyDown(event, movie.movieCode)},
                    React.createElement(
                              PosterWrap,
                              null,
                              (movie.posterUrl ? (
                                                  React.createElement(
                                          "img",
                                          {"src":resolvePosterUrl(movie.posterUrl),"alt":movie.title}
                                        )
                                                ) : (
                                                  React.createElement(
                                          EmptyPoster,
                                          null,
                                          "포스터 준비 중"
                                        )
                                                )),
                              React.createElement(
                                          Rank,
                                          null,
                                          (movie.rank)
                                        ),
                              React.createElement(
                                          FavoriteStar,
                                          {"movie":movie}
                                        )
                            ),
                    React.createElement(
                              MovieInfo,
                              null,
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
    ),
  React.createElement(
      Section,
      null,
      React.createElement(
            SectionHeading,
            null,
            React.createElement(
                    "div",
                    null,
                    React.createElement(
                              "h2",
                              null,
                              "장르로 영화 찾기"
                            ),
                    React.createElement(
                              "p",
                              null,
                              "지금 끌리는 분위기를 선택해 보세요."
                            )
                  )
          ),
      React.createElement(
            GenreList,
            null,
            (genres.map((genre) => (
                        React.createElement(
                    "button",
                    {"key":genre,"type":"button","onClick":() =>
                                    navigate("/search?genre=" + encodeURIComponent(genre))},
                    (genre)
                  )
                      )))
          )
    ),
  React.createElement(
      Section,
      null,
      React.createElement(
            SectionHeading,
            null,
            React.createElement(
                    "div",
                    null,
                    React.createElement(
                              "h2",
                              null,
                              "관심 영화"
                            ),
                    React.createElement(
                              "p",
                              null,
                              "별표로 저장한 영화를 다시 만나보세요."
                            )
                  ),
            React.createElement(
                    "button",
                    {"type":"button","className":"home-favorites-more","onClick":() => navigate("/profile/favorites")},
                    "전체 보기 →"
                  )
          ),
      (favoriteMovies.length === 0 ? (
                React.createElement(
            "div",
            {"className":"home-favorites-empty"},
            "박스오피스 포스터의 별표를 눌러 관심 영화를 저장해 보세요."
          )
              ) : (
                React.createElement(
            MovieGrid,
            null,
            (favoriteMovies.slice(0, 5).map((movie) => (
                          React.createElement(
                    MovieCard,
                    {"key":movie.movieCode,"role":"button","tabIndex":0,"onClick":() => goToMovie(movie.movieCode),"onKeyDown":(event) =>
                                      handleMovieKeyDown(event, movie.movieCode)},
                    React.createElement(
                              PosterWrap,
                              null,
                              (movie.posterUrl ? (
                                                  React.createElement(
                                          "img",
                                          {"src":resolvePosterUrl(movie.posterUrl),"alt":movie.title}
                                        )
                                                ) : (
                                                  React.createElement(
                                          EmptyPoster,
                                          null,
                                          "포스터 준비 중"
                                        )
                                                )),
                              React.createElement(
                                          FavoriteStar,
                                          {"movie":movie,"active":true}
                                        )
                            ),
                    React.createElement(
                              MovieInfo,
                              null,
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
    ),
  React.createElement(
      BrandLine,
      null,
      React.createElement(
            "div",
            null,
            React.createElement(
                    "h2",
                    null,
                    "영화는 끝나도, 감상은 남습니다."
                  ),
            React.createElement(
                    "p",
                    null,
                    "당신의 영화 기록을 MOMO에 남겨보세요."
                  )
          )
    )
)
  );
}

export default Home;
