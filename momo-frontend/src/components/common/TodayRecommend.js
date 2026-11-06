import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../App.css";

const h = React.createElement;

const getYesterdayDate = () => {
  const date = new Date();
  date.setDate(date.getDate() - 1);

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}${month}${day}`;
};

const isValidMovieCode = (movieCode) => {
  return /^[0-9]{8}$/.test(String(movieCode || ""));
};

function TodayRecommend() {
  const [movie, setMovie] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    const controller = new AbortController();

    const fetchRecommendMovie = async () => {
      try {
        setLoading(true);
        setError(false);

        const targetDate = getYesterdayDate();

        const response = await fetch(
          `http://localhost:8111/api/boxoffice/daily?targetDate=${targetDate}`,
          { signal: controller.signal },
        );

        if (!response.ok) {
          throw new Error(`박스오피스 조회 실패: ${response.status}`);
        }

        const data = await response.json();

        if (!Array.isArray(data)) {
          throw new Error("박스오피스 응답 형식이 올바르지 않습니다.");
        }

        const recommendedMovie = data.find(
          (item) =>
            item &&
            Number(item.rank) === 1 &&
            isValidMovieCode(item.movieCode) &&
            typeof item.title === "string" &&
            item.title.trim().length > 0,
        );

        if (!recommendedMovie) {
          setMovie(null);
          return;
        }

        setMovie(recommendedMovie);
      } catch (requestError) {
        if (requestError.name !== "AbortError") {
          console.error("오늘의 추천작 조회 실패:", requestError);
          setError(true);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchRecommendMovie();

    return () => controller.abort();
  }, []);

  const releaseYear =
    movie && movie.releaseDate ? movie.releaseDate.substring(0, 4) : "";

  if (loading) {
    return h(
      "div",
      { className: "recommend-state" },
      "오늘의 추천작을 불러오는 중입니다.",
    );
  }

  if (error || !movie) {
    return h(
      "div",
      { className: "recommend-state" },
      "추천 영화를 불러오지 못했습니다.",
    );
  }

  return h(
    "section",
    { className: "recommend-banner" },

    movie.posterUrl
      ? h("img", {
          className: "recommend-poster",
          src: movie.posterUrl,
          alt: movie.title,
        })
      : h(
          "div",
          { className: "recommend-poster poster-placeholder" },
          "포스터 준비 중",
        ),

    h("div", {
      className: "recommend-overlay",
    }),

    h(
      "div",
      { className: "recommend-content" },

      h("span", { className: "recommend-label" }, "오늘의 추천작"),

      h("h2", { className: "recommend-title" }, movie.title),

      h(
        "p",
        { className: "recommend-info" },
        releaseYear ? `${releaseYear}년 개봉` : "개봉일 정보 없음",
        " · ",
        "박스오피스 1위",
      ),

      h(
        "button",
        {
          className: "recommend-button",
          type: "button",
          onClick: () => {
            if (!isValidMovieCode(movie.movieCode)) {
              return;
            }

            navigate(`/movie/${encodeURIComponent(movie.movieCode)}`);
          },
        },
        "영화 자세히 보기",
      ),
    ),
  );
}

export default TodayRecommend;
