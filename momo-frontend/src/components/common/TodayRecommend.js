import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../App.css";

const h = React.createElement;

function TodayRecommend() {
  const [movie, setMovie] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const navigate = useNavigate();

  // 오늘의 추천작 조회
  useEffect(() => {
    fetch("http://localhost:8111/api/movies/recommend")
      .then((response) => {
        if (!response.ok) {
          throw new Error("추천 영화 조회 실패");
        }

        return response.json();
      })
      .then((data) => {
        setMovie(data);
        setLoading(false);
      })
      .catch(() => {
        setError(true);
        setLoading(false);
      });
  }, []);

  // 개봉 연도 표시
  let releaseYear = "";

  if (movie && movie.releaseDate) {
    releaseYear = movie.releaseDate.substring(0, 4);
  }

  // 로딩 화면
  if (loading) {
    return h(
      "div",
      { className: "recommend-state" },
      "오늘의 추천작을 불러오는 중입니다.",
    );
  }

  // 오류 화면
  if (error || !movie) {
    return h(
      "div",
      { className: "recommend-state" },
      "추천 영화를 불러오지 못했습니다.",
    );
  }

  // 추천작 배너
  return h(
    "section",
    { className: "recommend-banner" },

    // 영화 포스터
    h("img", {
      className: "recommend-poster",
      src: movie.posterUrl,
      alt: movie.title,
    }),

    // 배너 어두운 효과
    h("div", {
      className: "recommend-overlay",
    }),

    // 영화 정보
    h(
      "div",
      { className: "recommend-content" },

      h("span", { className: "recommend-label" }, "오늘의 추천작"),

      h("h2", { className: "recommend-title" }, movie.title),

      h(
        "p",
        { className: "recommend-info" },
        releaseYear,
        movie.genre ? " · " + movie.genre : "",
      ),

      h(
        "button",
        {
          className: "recommend-button",
          onClick: () => navigate("/movie/" + movie.movieCode),
        },
        "영화 자세히 보기",
      ),
    ),
  );
}

export default TodayRecommend;
