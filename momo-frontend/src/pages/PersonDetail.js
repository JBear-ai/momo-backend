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

function PersonDetail() {
  const { type, identifier } = useParams();
  const navigate = useNavigate();

  const [person, setPerson] = useState(null);
  const [filmography, setFilmography] = useState([]);
  const [visibleCount, setVisibleCount] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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

    if (!movie?.tmdbId) {
      return;
    }

    try {
      setNavigatingMovieId(movie.tmdbId);

      // 제목 검색 대신 TMDb 고유 ID로 KOBIS 영화 코드를 확인한다.
      const response = await AxiosApi.resolveFilmographyMovie(movie.tmdbId);
      const movieCode = response?.data?.movieCode;

      if (!/^[A-Za-z0-9]{8}$/.test(String(movieCode || ""))) {
        throw new Error("정확한 MOMO 영화 코드를 확인하지 못했습니다.");
      }

      navigate(`/movie/${movieCode}`);
    } catch (navigationError) {
      const status = navigationError?.response?.status;

      // KOBIS에 등록되지 않았거나 동명 작품이 여러 개인 경우는
      // 필모그래피에서 정상적으로 발생할 수 있는 미연결 상태로 처리한다.
      if (status !== 404 && status !== 409) {
        console.error("영화 상세 페이지 이동 실패:", navigationError);
      }
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
                        onError: (event) => event.currentTarget.remove(),
                      })
                    : null,

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
