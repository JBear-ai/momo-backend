import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";

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
  ) return null;
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
  ) age--;
  return age >= 0 ? age : null;
}

function formatReleaseDate(value) {
  if (!value) return "";
  const date = parseDate(value);
  if (date) return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
  if (/^\d{4}$/.test(String(value))) return `${value}년`;
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

        if (!personData) throw new Error("인물 정보 응답이 올바르지 않습니다.");
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
        if (active) setLoading(false);
      }
    }

    fetchPersonDetail();
    return () => {
      active = false;
      controller.abort();
    };
  }, [type, identifier]);

  if (loading) {
    return React.createElement(
  "main",
  {"className":"movie-detail-page"},
  React.createElement(
      "p",
      null,
      "인물 정보를 불러오는 중입니다."
    )
);
  }

  const age = person?.birthday
    ? calculateAge(person.birthday, person.deathday)
    : null;

  return (
    React.createElement(
  "main",
  {"className":"movie-detail-page"},
  React.createElement(
      "button",
      {"className":"detail-back-button","onClick":() => navigate(-1)},
      "← 이전으로 돌아가기"
    ),
  (error && React.createElement(
      "p",
      {"className":"people-empty","role":"alert"},
      (error)
    )),
  (person && (
          React.createElement(
      "section",
      {"className":"movie-detail-header"},
      React.createElement(
            "div",
            {"className":"movie-detail-poster"},
            (person.profileUrl ? (
                          React.createElement(
                    "img",
                    {"src":person.profileUrl,"alt":person.name || "인물 사진"}
                  )
                        ) : (
                          React.createElement(
                    "div",
                    {"className":"poster-placeholder"},
                    React.createElement(
                              "span",
                              null,
                              "👤"
                            ),
                    React.createElement(
                              "p",
                              null,
                              (person.name)
                            )
                  )
                        ))
          ),
      React.createElement(
            "div",
            {"className":"movie-detail-info"},
            React.createElement(
                    "span",
                    {"className":"detail-rank"},
                    (type === "director" ? "감독 상세정보" : "출연 배우 상세정보")
                  ),
            React.createElement(
                    "h1",
                    null,
                    (person.name)
                  ),
            React.createElement(
                    "div",
                    {"className":"person-meta-details"},
                    (person.placeOfBirth && React.createElement(
                              "p",
                              null,
                              "출생지: ",
                              (person.placeOfBirth)
                            )),
                    (person.birthday && (
                                    React.createElement(
                              "p",
                              null,
                              "생년월일: ",
                              (formatPersonDate(person.birthday)),
                              (age !== null && ` (${age}세)`)
                            )
                                  )),
                    (person.deathday && React.createElement(
                              "p",
                              null,
                              "사망일: ",
                              (formatPersonDate(person.deathday))
                            )),
                    (person.knownForDepartment && React.createElement(
                              "p",
                              null,
                              "분야: ",
                              (person.knownForDepartment)
                            ))
                  ),
            React.createElement(
                    "p",
                    null,
                    (person.biography || "등록된 인물 소개가 없습니다.")
                  )
          )
    )
        )),
  React.createElement(
      "section",
      {"className":"detail-content-section"},
      React.createElement(
            "h2",
            null,
            "필모그래피 (",
            (filmography.length),
            ")"
          ),
      (filmography.length > 0 ? (
                React.createElement(
            React.Fragment,
            null,
            React.createElement(
                    "div",
                    {"className":"people-list"},
                    (filmography.slice(0, visibleCount).map((movie) => (
                                    React.createElement(
                              "article",
                              {"className":"person-chip","key":movie.tmdbId},
                              (movie.posterUrl ? (
                                                  React.createElement(
                                          "img",
                                          {"src":movie.posterUrl,"alt":movie.title || "작품 포스터","loading":"lazy"}
                                        )
                                                ) : (
                                                  React.createElement(
                                          "div",
                                          null,
                                          "🎬"
                                        )
                                                )),
                              React.createElement(
                                          "div",
                                          null,
                                          React.createElement(
                                                        "strong",
                                                        null,
                                                        (movie.title || "제목 없음")
                                                      ),
                                          (movie.releaseDate && (
                                                                React.createElement(
                                                        "span",
                                                        null,
                                                        (formatReleaseDate(movie.releaseDate))
                                                      )
                                                              )),
                                          (movie.role && (
                                                                React.createElement(
                                                        "span",
                                                        {"className":"person-role"},
                                                        "배역: ",
                                                        (movie.role)
                                                      )
                                                              ))
                                        )
                            )
                                  )))
                  ),
            (visibleCount < filmography.length && (
                          React.createElement(
                    "div",
                    null,
                    React.createElement(
                              "button",
                              {"type":"button","onClick":() => setVisibleCount((count) => count + 10)},
                              "더보기 (",
                              (Math.min(visibleCount, filmography.length)),
                              " / ",
                              (filmography.length),
                              ")"
                            )
                  )
                        ))
          )
              ) : (
                React.createElement(
            "p",
            {"className":"people-empty"},
            (error ? "필모그래피 정보를 불러오지 못했습니다." : "참여한 작품 정보가 아직 연결되지 않았습니다.")
          )
              ))
    )
)
  );
}

export default PersonDetail;
