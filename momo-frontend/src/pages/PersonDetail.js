import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";

function calculateAge(birthday) {
  if (!birthday) return null;

  const birthDate = new Date(birthday);
  if (Number.isNaN(birthDate.getTime())) return null;

  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();

  if (
    monthDiff < 0 ||
    (monthDiff === 0 && today.getDate() < birthDate.getDate())
  ) {
    age--;
  }

  return age;
}

function PersonDetail() {
  const { type, identifier } = useParams();
  const navigate = useNavigate();

  const [person, setPerson] = useState(null);
  const [filmography, setFilmography] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function fetchPersonDetail() {
      try {
        setLoading(true);
        setError("");

        const response = await AxiosApi.getPersonDetail(type, identifier);
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
      } catch (err) {
        if (!active) return;

        console.error("인물 정보 조회 실패:", err);
        setPerson({ name: identifier });
        setFilmography([]);
        setError(
          err?.response?.data?.message ||
            "인물 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.",
        );
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    if (identifier) {
      fetchPersonDetail();
    }

    return () => {
      active = false;
    };
  }, [type, identifier]);

  if (loading) {
    return (
      <main className="movie-detail-page">
        <p>인물 정보를 불러오는 중입니다.</p>
      </main>
    );
  }

  const age = person?.birthday ? calculateAge(person.birthday) : null;

  return (
    <main className="movie-detail-page">
      <button className="detail-back-button" onClick={() => navigate(-1)}>
        ← 이전으로 돌아가기
      </button>

      {error && (
        <p className="people-empty" role="alert">
          {error}
        </p>
      )}

      <section className="movie-detail-header">
        <div className="movie-detail-poster">
          {person?.profileUrl ? (
            <img src={person.profileUrl} alt={person.name || "인물 사진"} />
          ) : (
            <div className="poster-placeholder">
              <span>👤</span>
              <p>{person?.name}</p>
            </div>
          )}
        </div>

        <div className="movie-detail-info">
          <span className="detail-rank">
            {type === "director" ? "감독 상세정보" : "출연 배우 상세정보"}
          </span>

          <h1>{person?.name}</h1>

          <div
            className="person-meta-details"
            style={{ margin: "12px 0", lineHeight: "1.6" }}
          >
            {person?.placeOfBirth && <p>출생지: {person.placeOfBirth}</p>}

            {person?.birthday && (
              <p>
                생년월일: {person.birthday}
                {age !== null && " (" + age + "세)"}
              </p>
            )}

            {person?.deathday && <p>사망일: {person.deathday}</p>}

            {person?.knownForDepartment && (
              <p>분야: {person.knownForDepartment}</p>
            )}
          </div>

          <p>{person?.biography || "등록된 인물 소개가 없습니다."}</p>
        </div>
      </section>

      <section className="detail-content-section">
        <h2>필모그래피 ({filmography.length})</h2>

        {filmography.length > 0 ? (
          <div className="people-list">
            {filmography.map((movie) => {
              const releaseYear = movie.releaseDate
                ? movie.releaseDate.substring(0, 4)
                : "";

              return (
                <div
                  className="person-chip"
                  key={movie.tmdbId}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "10px",
                  }}
                >
                  {movie.posterUrl ? (
                    <img
                      src={movie.posterUrl}
                      alt={movie.title || "작품 포스터"}
                      style={{
                        width: "30px",
                        height: "42px",
                        objectFit: "cover",
                        borderRadius: "3px",
                      }}
                    />
                  ) : (
                    <span style={{ fontSize: "18px" }}>🎬</span>
                  )}

                  <span>
                    {movie.title || "제목 없음"}
                    {releaseYear && " (" + releaseYear + ")"}
                    {movie.role && (
                      <span className="person-role"> ({movie.role})</span>
                    )}
                  </span>
                </div>
              );
            })}
          </div>
        ) : (
          <p className="people-empty">
            참여한 작품 정보가 아직 연결되지 않았습니다.
          </p>
        )}
      </section>
    </main>
  );
}

export default PersonDetail;
