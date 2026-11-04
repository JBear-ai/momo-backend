import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import TodayRecommend from "../components/common/TodayRecommend";
import FavoriteStar from "../components/common/FavoriteStar";
import { getFavoriteMovies } from "../utils/favorites";

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

const h = React.createElement;

// =========================================================
// 1. 어제 날짜 계산
// =========================================================

const getYesterdayDate = () => {
  const date = new Date();
  date.setDate(date.getDate() - 1);

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}${month}${day}`;
};

// =========================================================
// 2. 메인 페이지
// =========================================================

const Home = () => {
  const navigate = useNavigate();

  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  // 관심 영화 목록
  const [favoriteMovies, setFavoriteMovies] = useState(() =>
    getFavoriteMovies(),
  );

  // =========================================================
  // 3. 관심 영화 목록 실시간 갱신
  // =========================================================

  useEffect(() => {
    const updateFavorites = (event) => {
      setFavoriteMovies(event.detail || getFavoriteMovies());
    };

    window.addEventListener("momo-favorites-changed", updateFavorites);

    return () => {
      window.removeEventListener("momo-favorites-changed", updateFavorites);
    };
  }, []);

  // =========================================================
  // 4. 일일 박스오피스 조회
  // =========================================================

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

  // =========================================================
  // 5. 영화 상세 페이지 이동
  // =========================================================

  const goToMovie = (movieCode) => {
    if (!movieCode) return;

    navigate("/movie/" + movieCode);
  };

  // =========================================================
  // 6. 영화 카드 키보드 접근 처리
  // =========================================================

  const handleMovieKeyDown = (event, movieCode) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      goToMovie(movieCode);
    }
  };

  // =========================================================
  // 7. 장르 목록
  // =========================================================

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

  // =========================================================
  // 8. 화면 출력
  // =========================================================

  return h(
    HomePage,
    null,

    // =======================================================
    // 오늘의 추천 영화
    // =======================================================

    h(
      FeatureSection,
      null,

      h(
        FeatureInfo,
        null,

        h(Eyebrow, null, "MOMO · 오늘의 영화"),

        h(FeatureTitle, null, "오늘은 어떤 영화를"),

        h(FeatureTitle, null, "기억하고 싶나요?"),

        h(
          FeatureDescription,
          null,
          "영화를 보고 떠오른 생각을 기록하고,",
          h("br"),
          "다른 사람의 감상도 함께 살펴보세요.",
        ),
      ),

      h(FeatureVisual, null, h(TodayRecommend)),
    ),

    // =======================================================
    // 오늘의 박스오피스
    // =======================================================

    h(
      Section,
      null,

      h(
        SectionHeading,
        null,

        h(
          "div",
          null,

          h("h2", null, "오늘의 박스오피스"),

          h("p", null, "가장 많은 관객이 선택한 영화"),
        ),

        h(MoreText, null, "일일 관객 순위"),
      ),

      loading
        ? h("p", null, "영화 정보를 불러오는 중입니다.")
        : movies.length === 0
          ? h("p", null, "현재 표시할 영화가 없습니다.")
          : h(
              MovieGrid,
              null,

              movies.slice(0, 10).map((movie, index) =>
                h(
                  MovieCard,
                  {
                    key: movie.movieCode || index,
                    role: "button",
                    tabIndex: 0,

                    onClick: () => goToMovie(movie.movieCode),

                    onKeyDown: (event) =>
                      handleMovieKeyDown(event, movie.movieCode),
                  },

                  h(
                    PosterWrap,
                    null,

                    movie.posterUrl
                      ? h("img", {
                          src: movie.posterUrl,
                          alt: movie.title,
                        })
                      : h(EmptyPoster, null, "포스터 준비 중"),

                    h(Rank, null, movie.rank),

                    h(FavoriteStar, {
                      movie,
                    }),
                  ),

                  h(
                    MovieInfo,
                    null,

                    h("h3", null, movie.title),

                    h("p", null, movie.releaseDate || "개봉일 정보 없음"),
                  ),
                ),
              ),
            ),
    ),

    // =======================================================
    // 장르로 영화 찾기
    // =======================================================

    h(
      Section,
      null,

      h(
        SectionHeading,
        null,

        h(
          "div",
          null,

          h("h2", null, "장르로 영화 찾기"),

          h("p", null, "지금 끌리는 분위기를 선택해 보세요."),
        ),
      ),

      h(
        GenreList,
        null,

        genres.map((genre) =>
          h(
            "button",
            {
              key: genre,
              type: "button",

              onClick: () =>
                navigate("/search?genre=" + encodeURIComponent(genre)),
            },

            genre,
          ),
        ),
      ),
    ),

    // =======================================================
    // 관심 영화
    // =======================================================

    h(
      Section,
      null,

      h(
        SectionHeading,
        null,

        h(
          "div",
          null,

          h("h2", null, "관심 영화"),

          h("p", null, "별표로 저장한 영화를 다시 만나보세요."),
        ),

        h(
          "button",
          {
            type: "button",
            className: "home-favorites-more",

            onClick: () => navigate("/profile/favorites"),
          },

          "전체 보기 →",
        ),
      ),

      favoriteMovies.length === 0
        ? h(
            "div",
            {
              className: "home-favorites-empty",
            },

            "박스오피스 포스터의 별표를 눌러 관심 영화를 저장해 보세요.",
          )
        : h(
            MovieGrid,
            null,

            favoriteMovies.slice(0, 5).map((movie) =>
              h(
                MovieCard,
                {
                  key: movie.movieCode,
                  role: "button",
                  tabIndex: 0,

                  onClick: () => goToMovie(movie.movieCode),

                  onKeyDown: (event) =>
                    handleMovieKeyDown(event, movie.movieCode),
                },

                h(
                  PosterWrap,
                  null,

                  movie.posterUrl
                    ? h("img", {
                        src: movie.posterUrl,
                        alt: movie.title,
                      })
                    : h(EmptyPoster, null, "포스터 준비 중"),

                  h(FavoriteStar, {
                    movie,
                    active: true,
                  }),
                ),

                h(
                  MovieInfo,
                  null,

                  h("h3", null, movie.title),

                  h("p", null, movie.releaseDate || "개봉일 정보 없음"),
                ),
              ),
            ),
          ),
    ),

    // =======================================================
    // 브랜드 메시지
    // =======================================================

    h(
      BrandLine,
      null,

      h(
        "div",
        null,

        h("h2", null, "영화는 끝나도, 감상은 남습니다."),

        h("p", null, "당신의 영화 기록을 MOMO에 남겨보세요."),
      ),
    ),
  );
};

export default Home;
