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
    return <main className="movie-detail-page"><p>인물 정보를 불러오는 중입니다.</p></main>;
  }

  const age = person?.birthday
    ? calculateAge(person.birthday, person.deathday)
    : null;

  return (
    <main className="movie-detail-page">
      <button className="detail-back-button" onClick={() => navigate(-1)}>
        ← 이전으로 돌아가기
      </button>

      {error && <p className="people-empty" role="alert">{error}</p>}

      {person && (
        <section className="movie-detail-header">
          <div className="movie-detail-poster">
            {person.profileUrl ? (
              <img src={person.profileUrl} alt={person.name || "인물 사진"} />
            ) : (
              <div className="poster-placeholder">
                <span>👤</span>
                <p>{person.name}</p>
              </div>
            )}
          </div>

          <div className="movie-detail-info">
            <span className="detail-rank">
              {type === "director" ? "감독 상세정보" : "출연 배우 상세정보"}
            </span>
            <h1>{person.name}</h1>
            <div className="person-meta-details" style={{ margin: "12px 0", lineHeight: "1.6" }}>
              {person.placeOfBirth && <p>출생지: {person.placeOfBirth}</p>}
              {person.birthday && (
                <p>
                  생년월일: {formatPersonDate(person.birthday)}
                  {age !== null && ` (${age}세)`}
                </p>
              )}
              {person.deathday && <p>사망일: {formatPersonDate(person.deathday)}</p>}
              {person.knownForDepartment && <p>분야: {person.knownForDepartment}</p>}
            </div>
            <p>{person.biography || "등록된 인물 소개가 없습니다."}</p>
          </div>
        </section>
      )}

      <section className="detail-content-section">
        <h2>필모그래피 ({filmography.length})</h2>
        {filmography.length > 0 ? (
          <>
            <div
              className="people-list"
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
                gap: "20px",
              }}
            >
              {filmography.slice(0, visibleCount).map((movie) => (
                <article
                  className="person-chip"
                  key={movie.tmdbId}
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "stretch",
                    padding: "12px",
                    borderRadius: "8px",
                    backgroundColor: "#fff",
                    border: "1px solid #eaeaea",
                    boxShadow: "0 2px 4px rgba(0,0,0,0.04)",
                    gap: "10px",
                  }}
                >
                  {movie.posterUrl ? (
                    <img
                      src={movie.posterUrl}
                      alt={movie.title || "작품 포스터"}
                      loading="lazy"
                      style={{
                        width: "100%",
                        height: "250px",
                        objectFit: "cover",
                        borderRadius: "6px",
                      }}
                    />
                  ) : (
                    <div
                      style={{
                        width: "100%",
                        height: "250px",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        backgroundColor: "#f0f0f0",
                        borderRadius: "6px",
                        fontSize: "32px",
                      }}
                    >🎬</div>
                  )}
                  <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                    <strong style={{ fontSize: "15px", lineHeight: "1.3" }}>
                      {movie.title || "제목 없음"}
                    </strong>
                    {movie.releaseDate && (
                      <span style={{ fontSize: "13px", color: "#666" }}>
                        {formatReleaseDate(movie.releaseDate)}
                      </span>
                    )}
                    {movie.role && (
                      <span className="person-role" style={{ fontSize: "13px", color: "#444" }}>
                        배역: {movie.role}
                      </span>
                    )}
                  </div>
                </article>
              ))}
            </div>
            {visibleCount < filmography.length && (
              <div style={{ textAlign: "center", marginTop: "30px" }}>
                <button
                  type="button"
                  onClick={() => setVisibleCount((count) => count + 10)}
                  style={{
                    padding: "12px 24px",
                    cursor: "pointer",
                    borderRadius: "6px",
                    border: "1px solid #ccc",
                    backgroundColor: "#f9f9f9",
                    fontSize: "15px",
                    fontWeight: "600",
                  }}
                >
                  더보기 ({Math.min(visibleCount, filmography.length)} / {filmography.length})
                </button>
              </div>
            )}
          </>
        ) : (
          <p className="people-empty">
            {error ? "필모그래피 정보를 불러오지 못했습니다." : "참여한 작품 정보가 아직 연결되지 않았습니다."}
          </p>
        )}
      </section>
    </main>
  );
}

export default PersonDetail;
