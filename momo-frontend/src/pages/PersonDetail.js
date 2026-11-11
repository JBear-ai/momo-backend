import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";
import { resolvePosterUrl } from "../utils/mediaUrl";

function parseDate(value) {
  if (!value) return null;

  const normalized = String(value).trim().replace(/\./g, "-");
  const match = normalized.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);

  if (!match) return null;

  const [, year, month, day] = match.map(Number);
  const date = new Date(year, month - 1, day);

  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return null;
  }

  return date;
}

function formatPersonDate(value) {
  const date = parseDate(value);

  if (!date) return value || "";

  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
}

function calculateAge(birthday, deathday) {
  const birth = parseDate(birthday);

  if (!birth) return null;

  const end = parseDate(deathday) || new Date();

  let age = end.getFullYear() - birth.getFullYear();

  if (
    end.getMonth() < birth.getMonth() ||
    (end.getMonth() === birth.getMonth() && end.getDate() < birth.getDate())
  ) {
    age--;
  }

  return age >= 0 ? age : null;
}

function formatReleaseDate(value) {
  if (!value) return "";

  const date = parseDate(value);

  if (date) {
    return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
  }

  if (/^\d{4}$/.test(String(value))) {
    return `${value}년`;
  }

  return value;
}

// 영화 제목 비교용 정규화
function normalizeTitle(title) {
  return String(title || "")
    .normalize("NFKC")
    .replace(/[\s\p{P}\p{S}]/gu, "")
    .toLowerCase();
}

function PersonDetail() {
  const { type, identifier } = useParams();
  const navigate = useNavigate();

  const [person, setPerson] = useState(null);
  const [filmography, setFilmography] = useState([]);
  const [visibleCount, setVisibleCount] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [movieNavigationError, setMovieNavigationError] = useState("");
  const [navigatingMovieId, setNavigatingMovieId] = useState(null);

  // 인물 상세정보 조회
  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    async function fetchPersonDetail() {
      if (!identifier || !["director", "actor"].includes(type)) {
        setError("인물 정보 주소가 올바르지 않습니다.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError("");
        setPerson(null);
        setFilmography([]);
        setVisibleCount(10);
        setMovieNavigationError("");

        const response = await AxiosApi.getPersonDetail(
          type,
          identifier,
          controller.signal,
        );

        const personData = response?.data?.personInfo;
        const movies = response?.data?.filmography;

        if (!personData) {
          throw new Error("인물 정보 응답이 올바르지 않습니다.");
        }

        if (!active) return;

        setPerson({
          tmdbId: personData.tmdbId,
          name: personData.name || identifier,
          profileUrl: personData.profileUrl || null,
          birthday: personData.birthday || null,
          deathday: personData.deathday || null,
          placeOfBirth: personData.placeOfBirth || null,
          biography: personData.biography || null,
          knownForDepartment: personData.knownForDepartment || null,
        });

        setFilmography(Array.isArray(movies) ? movies : []);
      } catch (requestError) {
        if (!active || requestError.code === "ERR_CANCELED") return;

        console.error("인물 정보 조회 실패:", requestError);

        setError(
          requestError?.response?.data?.message ||
            requestError.message ||
            "인물 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.",
        );
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    fetchPersonDetail();

    return () => {
      active = false;
      controller.abort();
    };
  }, [type, identifier]);

  // 필모그래피 작품을 MOMO 영화 상세 페이지와 연결
  const handleFilmographyClick = async (movie) => {
    if (navigatingMovieId !== null) return;

    if (!movie?.title) {
      setMovieNavigationError("작품 제목을 확인할 수 없습니다.");
      return;
    }

    const movieId = movie.tmdbId ?? movie.title;

    try {
      setMovieNavigationError("");
      setNavigatingMovieId(movieId);

      // 백엔드 검색 페이지는 1부터 시작
      const response = await AxiosApi.movieSearch(movie.title, 1, 20);

      const results = Array.isArray(response?.data?.content)
        ? response.data.content
        : [];

      if (results.length === 0) {
        throw new Error("MOMO에서 해당 영화를 찾을 수 없습니다.");
      }

      const targetTitle = normalizeTitle(movie.title);

      // 제목이 정확히 일치하는 영화만 선택
      const exactMatches = results.filter(
        (item) => normalizeTitle(item.title) === targetTitle,
      );

      if (exactMatches.length === 0) {
        throw new Error("MOMO에서 제목이 일치하는 영화를 찾을 수 없습니다.");
      }

      // 개봉 연도까지 비교하여 동명 영화 구분
      const movieYear = String(movie.releaseDate || "").slice(0, 4);

      let matchedMovie;

      if (/^\d{4}$/.test(movieYear)) {
        matchedMovie = exactMatches.find(
          (item) => String(item.releaseDate || "").slice(0, 4) === movieYear,
        );

        if (!matchedMovie) {
          throw new Error(
            "제목은 일치하지만 개봉 연도가 일치하는 영화를 찾지 못했습니다.",
          );
        }
      } else if (exactMatches.length === 1) {
        matchedMovie = exactMatches[0];
      } else {
        throw new Error(
          "동일한 제목의 영화가 여러 개 있어 정확한 작품을 찾을 수 없습니다.",
        );
      }

      const movieCode = matchedMovie.movieCode;

      if (!movieCode) {
        throw new Error("영화 상세 페이지에 필요한 영화 코드가 없습니다.");
      }

      navigate(`/movie/${encodeURIComponent(movieCode)}`);
    } catch (navigationError) {
      console.error("영화 상세 페이지 이동 실패:", navigationError);

      setMovieNavigationError(
        navigationError?.response?.data?.message ||
          navigationError.message ||
          "영화 정보를 찾을 수 없습니다. 잠시 후 다시 시도해주세요.",
      );
    } finally {
      setNavigatingMovieId(null);
    }
  };

  // Enter 또는 Space 키로 작품 선택
  const handleFilmographyKeyDown = (event, movie) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handleFilmographyClick(movie);
    }
  };

  if (loading) {
    return React.createElement(
      "main",
      { className: "movie-detail-page" },
      React.createElement("p", null, "인물 정보를 불러오는 중입니다."),
    );
  }

  const age = person?.birthday
    ? calculateAge(person.birthday, person.deathday)
    : null;

  return React.createElement(
    "main",
    { className: "movie-detail-page" },

    // 이전 페이지 이동
    React.createElement(
      "button",
      {
        className: "detail-back-button",
        onClick: () => navigate(-1),
      },
      "← 이전으로 돌아가기",
    ),

    // 인물 정보 오류
    error &&
      React.createElement(
        "p",
        {
          className: "people-empty",
          role: "alert",
        },
        error,
      ),

    // 인물 기본 정보
    person &&
      React.createElement(
        "section",
        { className: "movie-detail-header" },

        React.createElement(
          "div",
          { className: "movie-detail-poster" },

          person.profileUrl
            ? React.createElement("img", {
                src: person.profileUrl,
                alt: person.name || "인물 사진",
              })
            : React.createElement(
                "div",
                { className: "poster-placeholder" },
                React.createElement("span", null, "👤"),
                React.createElement("p", null, person.name),
              ),
        ),

        React.createElement(
          "div",
          { className: "movie-detail-info" },

          React.createElement(
            "span",
            { className: "detail-rank" },
            type === "director" ? "감독 상세정보" : "출연 배우 상세정보",
          ),

          React.createElement("h1", null, person.name),

          React.createElement(
            "div",
            { className: "person-meta-details" },

            person.placeOfBirth &&
              React.createElement("p", null, "출생지: ", person.placeOfBirth),

            person.birthday &&
              React.createElement(
                "p",
                null,
                "생년월일: ",
                formatPersonDate(person.birthday),
                age !== null ? ` (${age}세)` : "",
              ),

            person.deathday &&
              React.createElement(
                "p",
                null,
                "사망일: ",
                formatPersonDate(person.deathday),
              ),

            person.knownForDepartment &&
              React.createElement(
                "p",
                null,
                "분야: ",
                person.knownForDepartment,
              ),
          ),

          React.createElement(
            "p",
            null,
            person.biography || "등록된 인물 소개가 없습니다.",
          ),
        ),
      ),

    // 필모그래피
    React.createElement(
      "section",
      { className: "detail-content-section" },

      React.createElement("h2", null, "필모그래피 (", filmography.length, ")"),

      movieNavigationError &&
        React.createElement(
          "p",
          {
            className: "people-empty",
            role: "alert",
          },
          movieNavigationError,
        ),

      filmography.length > 0
        ? React.createElement(
            React.Fragment,
            null,

            React.createElement(
              "div",
              { className: "people-list" },

              filmography.slice(0, visibleCount).map((movie, index) => {
                const movieId = movie.tmdbId ?? `${movie.title}-${index}`;

                const isNavigating = navigatingMovieId === movieId;

                return React.createElement(
                  "article",
                  {
                    className: "person-chip",
                    key: movieId,
                    onClick: () => handleFilmographyClick(movie),
                    onKeyDown: (event) =>
                      handleFilmographyKeyDown(event, movie),
                    role: "button",
                    tabIndex: 0,
                    style: { cursor: "pointer" },
                    "aria-label": `${movie.title || "작품"} 영화 상세정보 보기`,
                    "aria-busy": isNavigating,
                  },

                  // 작품 포스터
                  movie.posterUrl
                    ? React.createElement("img", {
                        src: resolvePosterUrl(movie.posterUrl),
                        alt: movie.title || "작품 포스터",
                        loading: "lazy",
                      })
                    : React.createElement("div", null, "🎬"),

                  // 작품 정보
                  React.createElement(
                    "div",
                    null,

                    React.createElement(
                      "strong",
                      null,
                      isNavigating
                        ? "영화 검색 중..."
                        : movie.title || "제목 없음",
                    ),

                    movie.releaseDate &&
                      React.createElement(
                        "span",
                        null,
                        formatReleaseDate(movie.releaseDate),
                      ),

                    movie.role &&
                      React.createElement(
                        "span",
                        { className: "person-role" },
                        "배역: ",
                        movie.role,
                      ),
                  ),
                );
              }),
            ),

            // 필모그래피 더보기
            visibleCount < filmography.length &&
              React.createElement(
                "div",
                null,

                React.createElement(
                  "button",
                  {
                    type: "button",
                    onClick: () => setVisibleCount((count) => count + 10),
                  },
                  "더보기 (",
                  Math.min(visibleCount, filmography.length),
                  " / ",
                  filmography.length,
                  ")",
                ),
              ),
          )
        : React.createElement(
            "p",
            { className: "people-empty" },
            error
              ? "필모그래피 정보를 불러오지 못했습니다."
              : "참여한 작품 정보가 아직 연결되지 않았습니다.",
          ),
    ),
  );
}

export default PersonDetail;
