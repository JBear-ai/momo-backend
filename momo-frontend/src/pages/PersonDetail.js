import SafeImage from "../components/common/SafeImage";
import React, { useEffect, useRef, useState } from "react";
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
  const filmographyControllerRef = useRef(null);

  const [person, setPerson] = useState(null);
  const [filmography, setFilmography] = useState([]);
  const [visibleCount, setVisibleCount] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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

        // 숫자형 식별자는 TMDb 인물 고유 ID로 조회해 동명이인 오연결을 방지한다.
        const response = /^\d+$/.test(identifier)
          ? await AxiosApi.getPersonDetailById(type, identifier, controller.signal)
          : await AxiosApi.getPersonDetail(type, identifier, controller.signal);

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
      filmographyControllerRef.current?.abort();
    };
  }, [type, identifier]);

  // 필모그래피의 TMDb ID를 먼저 MOMO 영화 코드와 연결하고, 미연결이면 같은 영화 경로로 연다.
  const handleFilmographyClick = async (movie) => {
    const tmdbId = Number(movie?.tmdbId);
    if (!Number.isSafeInteger(tmdbId) || tmdbId <= 0) return;

    // 빠르게 다른 작품을 선택하면 이전 작품 연결 요청은 취소한다.
    filmographyControllerRef.current?.abort();
    const controller = new AbortController();
    filmographyControllerRef.current = controller;

    try {
      const response = await AxiosApi.resolveFilmographyMovie(
        tmdbId,
        controller.signal,
      );

      if (controller.signal.aborted) return;

      const movieCode = response?.data?.movieCode;
      navigate(`/movie/${encodeURIComponent(movieCode || tmdbId)}`);
    } catch (error) {
      // KOBIS 미등록 영화도 TMDb ID로 통합 영화 상세 경로에서 조회한다.
      if (
        !controller.signal.aborted &&
        error.code !== "ERR_CANCELED" &&
        error.name !== "CanceledError"
      ) {
        navigate(`/movie/${tmdbId}`);
      }
    } finally {
      if (filmographyControllerRef.current === controller) {
        filmographyControllerRef.current = null;
      }
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

          React.createElement(SafeImage, {
            src: person.profileUrl,
            alt: person.name || "인물 사진",
            fallbackClassName: "poster-placeholder",
            fallbackText: person.name || "인물 사진",
          }),
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
                  },

                  // 작품 포스터
                  React.createElement(SafeImage, {
                    src: resolvePosterUrl(movie.posterUrl),
                    alt: movie.title || "작품 포스터",
                    fallback: React.createElement(
                      "div",
                      { className: "poster-placeholder" },
                      "MOMO"
                    ),
                    loading: "lazy",
                  }),

                  // 작품 정보
                  React.createElement(
                    "div",
                    null,

                    React.createElement(
                      "strong",
                      null,
                      movie.title || "제목 없음",
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
