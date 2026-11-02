import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";

// 숫자에 천 단위 쉼표 추가
function formatNumber(value) {
  return Number(value || 0).toLocaleString("ko-KR");
}

// 감독과 배우 데이터를 화면에서 사용할 수 있는 형태로 변환
function normalizePeople(people) {
  if (!people) return [];

  if (typeof people === "string") {
    return people
      .split(",")
      .map((name) => name.trim())
      .filter(Boolean)
      .map((name) => ({
        name,
        role: "",
        works: [],
      }));
  }

  if (!Array.isArray(people)) {
    people = [people];
  }

  return people.map((person) => {
    if (typeof person === "string") {
      return {
        name: person,
        role: "",
        works: [],
      };
    }

    return {
      name:
        person.name ||
        person.peopleNm ||
        person.actorName ||
        person.directorName ||
        "이름 없음",
      role:
        person.role || person.cast || person.character || person.castName || "",
      works: person.works || person.filmography || [],
    };
  });
}

function MovieDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  // 영화 상세정보
  const [movie, setMovie] = useState(null);

  // 데이터 조회 상태
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // 리뷰 관련 상태
  const [reviews, setReviews] = useState([]);
  const [reviewText, setReviewText] = useState("");
  const [reaction, setReaction] = useState("");
  const [isReviewOpen, setIsReviewOpen] = useState(false);

  // 감독 및 배우 선택 상태
  const [selectedPerson, setSelectedPerson] = useState(null);

  // 영화 상세정보 및 포스터 조회
  useEffect(() => {
    async function fetchMovieDetail() {
      try {
        setLoading(true);
        setError("");

        // 1. 영화 상세정보 조회
        const detailResponse = await fetch(
          `http://localhost:8111/api/movies/${id}`,
        );

        if (!detailResponse.ok) {
          throw new Error(`영화 상세정보 조회 실패: ${detailResponse.status}`);
        }

        const detailData = await detailResponse.json();

        // 2. 영화 포스터 조회
        let posterUrl = "";

        try {
          const posterResponse = await fetch(
            `http://localhost:8111/api/movies/${id}/poster`,
          );

          if (posterResponse.ok) {
            const posterData = await posterResponse.json();
            posterUrl = posterData.posterUrl || "";
          } else {
            console.error("포스터 조회 실패:", posterResponse.status);
          }
        } catch (posterError) {
          console.error("포스터 조회 오류:", posterError);
        }

        // 3. 상세정보와 포스터를 하나의 객체로 저장
        setMovie({
          ...detailData,
          id: detailData.movieCode,
          posterUrl,
        });
      } catch (error) {
        console.error("영화 상세정보 조회 실패:", error);
        setError(error.message);
      } finally {
        setLoading(false);
      }
    }

    if (id) {
      fetchMovieDetail();
    }
  }, [id]);

  // 영화별 리뷰 목록
  const movieReviews = reviews.filter(
    (review) => review.movieId === movie?.movieCode,
  );

  // 좋아요 수
  const likeCount = movieReviews.filter(
    (review) => review.reaction === "like",
  ).length;

  // 싫어요 수
  const dislikeCount = movieReviews.filter(
    (review) => review.reaction === "dislike",
  ).length;

  // 전체 반응 수
  const totalReactions = likeCount + dislikeCount;

  // 관객 평점 계산
  const rating =
    totalReactions > 0
      ? ((likeCount / totalReactions) * 10).toFixed(1)
      : "평가 없음";

  // 리뷰 등록
  function addReview() {
    if (!reaction) {
      alert("좋아요 또는 싫어요를 선택해 주세요.");
      return;
    }

    if (!reviewText.trim()) {
      alert("리뷰 내용을 입력해 주세요.");
      return;
    }

    const newReview = {
      id: Date.now(),
      movieId: movie.movieCode,
      text: reviewText.trim(),
      reaction,
      date: new Date().toLocaleDateString("ko-KR"),
    };

    setReviews((prev) => [newReview, ...prev]);

    setReviewText("");
    setReaction("");
    setIsReviewOpen(false);
  }

  // 영화 정보 로딩 중
  if (loading) {
    return (
      <main className="movie-detail-page">
        <p>영화 정보를 불러오는 중입니다.</p>
      </main>
    );
  }

  // 영화 정보 조회 실패
  if (error || !movie) {
    return (
      <main className="movie-detail-page">
        <button className="detail-back-button" onClick={() => navigate("/")}>
          ← 메인으로 돌아가기
        </button>

        <p>{error || "영화 정보를 찾을 수 없습니다."}</p>
      </main>
    );
  }

  // 백엔드 DTO에 맞춰 감독과 배우 데이터 변환
  const directors = normalizePeople(movie.director);
  const actors = normalizePeople(movie.actors);

  return (
    <main className="movie-detail-page">
      {/* 메인으로 돌아가기 */}
      <button className="detail-back-button" onClick={() => navigate("/")}>
        ← 메인으로 돌아가기
      </button>

      {/* 영화 기본정보 */}
      <section className="movie-detail-header">
        {/* 영화 포스터 */}
        <div className="movie-detail-poster">
          {movie.posterUrl ? (
            <img src={movie.posterUrl} alt={movie.title} />
          ) : (
            <div className="poster-placeholder">
              <span>🎬</span>
              <p>{movie.title}</p>
            </div>
          )}
        </div>

        {/* 영화 정보 */}
        <div className="movie-detail-info">
          <span className="detail-rank">영화 상세정보</span>

          <h1>{movie.title}</h1>

          <p>영문 제목: {movie.titleEn || "정보 없음"}</p>

          <p>개봉일: {movie.releaseDate || "정보 없음"}</p>

          <p>
            상영시간:{" "}
            {movie.runningTime
              ? `${formatNumber(movie.runningTime)}분`
              : "정보 없음"}
          </p>

          <p>장르: {movie.genre || "정보 없음"}</p>

          <p>관람등급: {movie.watchGrade || "정보 없음"}</p>
        </div>
      </section>

      {/* 관객 평점 */}
      <section className="movie-rating-panel">
        <h2>관객 평점</h2>

        <div className="movie-rating-number">{rating}</div>

        <div className="detail-reaction-counts">
          <div className="detail-reaction-count">
            👍 좋아요 <strong>{likeCount}</strong>
          </div>

          <div className="detail-reaction-count">
            👎 싫어요 <strong>{dislikeCount}</strong>
          </div>
        </div>
      </section>

      {/* 감독 */}
      <section className="detail-content-section">
        <h2>감독</h2>

        {directors.length > 0 ? (
          <div className="people-list">
            {directors.map((person, index) => (
              <button
                className="person-chip"
                key={index}
                onClick={() =>
                  setSelectedPerson({
                    ...person,
                    type: "감독",
                  })
                }
              >
                {person.name}
              </button>
            ))}
          </div>
        ) : (
          <p className="people-empty">감독 정보가 아직 연결되지 않았습니다.</p>
        )}
      </section>

      {/* 출연 배우 */}
      <section className="detail-content-section">
        <h2>출연 배우</h2>

        {actors.length > 0 ? (
          <div className="people-list">
            {actors.map((person, index) => (
              <button
                className="person-chip"
                key={index}
                onClick={() =>
                  setSelectedPerson({
                    ...person,
                    type: "배우",
                  })
                }
              >
                {person.name}

                {person.role && (
                  <span className="person-role">({person.role})</span>
                )}
              </button>
            ))}
          </div>
        ) : (
          <p className="people-empty">배우 정보가 아직 연결되지 않았습니다.</p>
        )}
      </section>

      {/* 감독 및 배우 필모그래피 */}
      {selectedPerson && (
        <section className="person-filmography">
          <h2>{selectedPerson.name} 필모그래피</h2>

          <p>{selectedPerson.type}</p>

          {selectedPerson.works?.length > 0 ? (
            <ul className="filmography-list">
              {selectedPerson.works.map((work, index) => (
                <li key={index}>
                  {typeof work === "string"
                    ? work
                    : work.title || work.movieNm || "작품 정보"}
                </li>
              ))}
            </ul>
          ) : (
            <p>필모그래피 정보가 아직 연결되지 않았습니다.</p>
          )}

          <button onClick={() => setSelectedPerson(null)}>닫기</button>
        </section>
      )}

      {/* 리뷰 */}
      <section className="detail-content-section">
        <h2>리뷰</h2>

        <div className="review-write-section">
          {/* 리뷰 작성 열기 */}
          <button
            className="review-write-toggle"
            onClick={() => setIsReviewOpen((prev) => !prev)}
          >
            <span>{movie.title}</span>

            <span className="review-toggle-arrow">
              {isReviewOpen ? "▲" : "▼"}
            </span>
          </button>

          {/* 리뷰 작성 영역 */}
          {isReviewOpen && (
            <div className="review-write-content">
              <h3>이 영화는 어떠셨나요?</h3>

              {/* 좋아요 및 싫어요 */}
              <div className="reaction-buttons">
                <button
                  className={`reaction-button ${
                    reaction === "like" ? "selected-like" : ""
                  }`}
                  onClick={() => setReaction("like")}
                >
                  👍 좋아요
                </button>

                <button
                  className={`reaction-button ${
                    reaction === "dislike" ? "selected-dislike" : ""
                  }`}
                  onClick={() => setReaction("dislike")}
                >
                  👎 싫어요
                </button>
              </div>

              {/* 리뷰 내용 */}
              <textarea
                value={reviewText}
                onChange={(e) => setReviewText(e.target.value)}
                placeholder="영화에 대한 리뷰를 작성해 주세요."
                maxLength={1000}
              />

              {/* 리뷰 등록 */}
              <div className="review-form-actions">
                <span>{reviewText.length}/1000</span>

                <button onClick={addReview}>리뷰 등록</button>
              </div>
            </div>
          )}
        </div>

        {/* 리뷰 목록 */}
        {movieReviews.length > 0 ? (
          <div className="detail-review-list">
            {movieReviews.map((review) => (
              <article className="detail-review-card" key={review.id}>
                <div>
                  {review.reaction === "like" ? "👍 좋아요" : "👎 싫어요"}

                  <span>{review.date}</span>
                </div>

                <p>{review.text}</p>
              </article>
            ))}
          </div>
        ) : (
          <p className="people-empty">아직 작성된 리뷰가 없습니다.</p>
        )}
      </section>
    </main>
  );
}

export default MovieDetail;
