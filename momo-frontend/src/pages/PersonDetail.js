import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";

// 생년월일로부터 나이를 계산하는 안전한 함수
function calculateAge(birthday) {
  if (!birthday) return null;
  const birthDate = new Date(birthday);
  if (isNaN(birthDate.getTime())) return null;

  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const m = today.getMonth() - birthDate.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
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

  // 인물 상세 정보 및 필모그래피 데이터 조회
  useEffect(() => {
    async function fetchPersonDetail() {
      try {
        setLoading(true);
        setError("");

        const response = await AxiosApi.getPersonDetail(type, identifier);

        // 응답 데이터 방어 처리 및 매핑
        const personData =
          response?.data?.personInfo || response?.data?.person || {};
        const filmos =
          response?.data?.filmography || response?.data?.movies || [];

        setPerson({
          name: personData.name || decodeURIComponent(identifier),
          profileUrl: personData.profileUrl || personData.profile_path || null,
          birthday: personData.birthday || personData.birthDate || null,
          nationality:
            personData.nationality || personData.place_of_birth || null,
          agency: personData.agency || null,
          biography: personData.biography || personData.intro || null,
        });

        setFilmography(filmos);
      } catch (err) {
        console.error("인물 정보 조회 실패:", err);
        setPerson({ name: decodeURIComponent(identifier) });
        setFilmography([]);
      } finally {
        setLoading(false);
      }
    }

    if (identifier) {
      fetchPersonDetail();
    }
  }, [type, identifier]);

  if (loading) {
    return (
      <main className="movie-detail-page">
        <p>인물 정보를 불러오는 중입니다.</p>
      </main>
    );
  }

  // 나이 계산식 안전하게 수정
  const age = person?.birthday ? calculateAge(person.birthday) : null;

  return (
    <main className="movie-detail-page">
      <button className="detail-back-button" onClick={() => navigate(-1)}>
        ← 이전으로 돌아가기
      </button>

      {/* 인물 기본 정보 및 프로필 사진 */}
      <section className="movie-detail-header">
        <div className="movie-detail-poster">
          {person?.profileUrl ? (
            <img
              src={
                person.profileUrl.startsWith("http")
                  ? person.profileUrl
                  : `https://image.tmdb.org/t/p/w500${person.profileUrl}`
              }
              alt={person?.name || "인물 사진"}
            />
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

          {/* 메타 정보 (국적, 소속사, 생년월일, 나이 등) */}
          <div
            className="person-meta-details"
            style={{ margin: "12px 0", lineHeight: "1.6" }}
          >
            {person?.nationality && <p>국적/출생지: {person.nationality}</p>}
            {person?.agency && <p>소속사: {person.agency}</p>}
            {person?.birthday && (
              <p>
                생년월일: {person.birthday}{" "}
                {age !== null && !isNaN(age) ? `(${age}세)` : ""}
              </p>
            )}
          </div>

          <p>{person?.biography || "등록된 인물 소개가 없습니다."}</p>
        </div>
      </section>

      {/* 포스터와 연도가 포함된 필모그래피 목록 */}
      <section className="detail-content-section">
        <h2>필모그래피 ({filmography.length})</h2>

        {filmography.length > 0 ? (
          <div className="people-list">
            {filmography.map((movie, index) => {
              const releaseYear =
                movie?.releaseDate || movie?.release_date
                  ? String(movie.releaseDate || movie.release_date).substring(
                      0,
                      4,
                    )
                  : "";

              const posterImage = movie?.posterUrl || movie?.poster_path;

              return (
                <button
                  type="button"
                  className="person-chip"
                  key={`${movie?.movieCode || movie?.id || index}-${index}`}
                  onClick={() => {
                    const movieId = movie?.movieCode || movie?.id;
                    if (movieId) navigate(`/movie/${movieId}`);
                  }}
                  style={{ display: "flex", alignItems: "center", gap: "10px" }}
                >
                  {posterImage ? (
                    <img
                      src={
                        posterImage.startsWith("http")
                          ? posterImage
                          : `https://image.tmdb.org/t/p/w500${posterImage}`
                      }
                      alt={
                        movie?.title || movie?.original_title || "작품 포스터"
                      }
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
                    {movie?.title || movie?.original_title || "제목 없음"}{" "}
                    {releaseYear && `(${releaseYear})`}
                    {(movie?.role || movie?.character) && (
                      <span className="person-role">
                        {" "}
                        ({movie.role || movie.character} 역)
                      </span>
                    )}
                  </span>
                </button>
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
