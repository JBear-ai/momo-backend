import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";
import { formatReleaseDate } from "../utils/releaseDate";

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

  // 영화 평가 요약
  const [ratingSummary, setRatingSummary] = useState({
    likeCount: 0,
    dislikeCount: 0,
    totalCount: 0,
    positiveRate: null,
    myRating: null,
  });

  // 영화 평가 처리 상태
  const [isRatingSubmitting, setIsRatingSubmitting] = useState(false);
  const [ratingError, setRatingError] = useState("");

  // 리뷰 제목
  const [reviewTitle, setReviewTitle] = useState("");

  // 리뷰 내용
  const [reviewText, setReviewText] = useState("");

  // 리뷰 작성 영역 열림 여부
  const [isReviewOpen, setIsReviewOpen] = useState(false);

  // 리뷰 등록 중 여부
  const [isReviewSubmitting, setIsReviewSubmitting] = useState(false);

  // 리뷰 오류 메시지
  const [reviewError, setReviewError] = useState("");

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

  // 영화별 리뷰와 평가 조회
  useEffect(() => {
    const controller = new AbortController();

    async function fetchReviewsAndRating() {
      try {
        setReviewError("");
        setRatingError("");

        const [reviewsResponse, ratingResponse] = await Promise.all([
          AxiosApi.movieReviews(id, 0, controller.signal),
          AxiosApi.movieRating(id, controller.signal),
        ]);

        setReviews(reviewsResponse.data.content || []);
        setRatingSummary(ratingResponse.data);
      } catch (error) {
        if (error.name !== "CanceledError" && error.code !== "ERR_CANCELED") {
          console.error("리뷰 및 평점 조회 실패:", error);
          setReviewError("리뷰를 불러오지 못했습니다.");
          setRatingError("영화 평가를 불러오지 못했습니다.");
        }
      }
    }

    if (id) {
      fetchReviewsAndRating();
    }

    return () => controller.abort();
  }, [id]);

  // 좋아요 수
  const likeCount = ratingSummary.likeCount || 0;

  // 싫어요 수
  const dislikeCount = ratingSummary.dislikeCount || 0;

  // 내 영화 평가
  const myRating = ratingSummary.myRating;

  // 관객 평점 계산
  const rating =
    ratingSummary.positiveRate != null
      ? (ratingSummary.positiveRate / 10).toFixed(1)
      : "평가 없음";

  // 영화 평가 갱신
  async function refreshMovieRating() {
    const response = await AxiosApi.movieRating(movie.movieCode);
    setRatingSummary(response.data);
  }

  // 영화 좋아요 / 싫어요 선택 및 취소
  async function handleMovieRating(liked) {
    if (isRatingSubmitting) return;

    try {
      setIsRatingSubmitting(true);
      setRatingError("");

      // 이미 선택한 평가를 다시 누르면 취소
      if (myRating === liked) {
        await AxiosApi.movieRatingDelete(movie.movieCode);
      } else {
        // 새로운 평가 또는 기존 평가 변경
        await AxiosApi.movieRatingSave(movie.movieCode, liked);
      }

      // 서버에 저장된 최신 평가 정보 반영
      await refreshMovieRating();
    } catch (error) {
      console.error("영화 평가 처리 실패:", error);

      if (error.response?.status === 401) {
        setRatingError("영화 평가를 하려면 로그인이 필요합니다.");
      } else {
        setRatingError(
          error.response?.data?.message || "영화 평가 처리에 실패했습니다.",
        );
      }
    } finally {
      setIsRatingSubmitting(false);
    }
  }

  // 리뷰 등록
  async function addReview() {
    // 제목 검사
    if (!reviewTitle.trim()) {
      alert("리뷰 제목을 입력해 주세요.");
      return;
    }

    // 제목 길이 검사
    if (reviewTitle.trim().length > 100) {
      alert("리뷰 제목은 100자 이하로 입력해 주세요.");
      return;
    }

    // 리뷰 내용 검사
    if (!reviewText.trim()) {
      alert("리뷰 내용을 입력해 주세요.");
      return;
    }

    // 리뷰 내용 길이 검사
    if (reviewText.trim().length > 2000) {
      alert("리뷰 내용은 2000자 이하로 입력해 주세요.");
      return;
    }

    try {
      setIsReviewSubmitting(true);
      setReviewError("");

      // 제목과 내용만 서버에 전달
      const createResponse = await AxiosApi.reviewCreate(
        movie.movieCode,
        reviewTitle.trim(),
        reviewText.trim(),
      );

      // 새 리뷰를 목록 맨 앞에 추가
      setReviews((prev) => [createResponse.data, ...prev]);

      // 입력값 초기화
      setReviewTitle("");
      setReviewText("");

      // 작성 영역 닫기
      setIsReviewOpen(false);
    } catch (error) {
      console.error("리뷰 등록 실패:", error);

      if (error.response?.status === 401) {
        setReviewError("리뷰를 등록하려면 로그인이 필요합니다.");
      } else if (error.response?.status === 400) {
        setReviewError(
          error.response?.data?.message || "입력한 리뷰 정보를 확인해 주세요.",
        );
      } else if (error.response?.status === 409) {
        setReviewError("이미 해당 영화에 리뷰를 작성했습니다.");
      } else {
        setReviewError(
          error.response?.data?.message || "리뷰 등록에 실패했습니다.",
        );
      }
    } finally {
      setIsReviewSubmitting(false);
    }
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

  // 감독과 배우 데이터 변환
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

          <p>개봉일(KOBIS): {formatReleaseDate(movie.releaseDate)}</p>

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

      {/* 관객 평점 및 영화 평가 */}
      <section className="movie-rating-panel">
        <h2>관객 평점</h2>

        <div className="movie-rating-number">{rating}</div>

        <div className="detail-reaction-counts">
          <div className="detail-reaction-count">
            👍 좋아요 <strong>{formatNumber(likeCount)}</strong>
          </div>

          <div className="detail-reaction-count">
            👎 싫어요 <strong>{formatNumber(dislikeCount)}</strong>
          </div>
        </div>

        <p>이 영화에 대한 평가를 선택해 주세요.</p>

        <div className="reaction-buttons">
          <button
            type="button"
            className={`reaction-button ${
              myRating === true ? "selected-like" : ""
            }`}
            onClick={() => handleMovieRating(true)}
            disabled={isRatingSubmitting}
          >
            👍 좋아요
          </button>

          <button
            type="button"
            className={`reaction-button ${
              myRating === false ? "selected-dislike" : ""
            }`}
            onClick={() => handleMovieRating(false)}
            disabled={isRatingSubmitting}
          >
            👎 싫어요
          </button>
        </div>

        {myRating !== null && myRating !== undefined && (
          <p>
            내 평가: {myRating ? "좋아요" : "싫어요"} · 같은 버튼을 누르면
            취소됩니다.
          </p>
        )}

        {isRatingSubmitting && <p>평가를 저장하고 있습니다.</p>}

        {ratingError && <p className="people-empty">{ratingError}</p>}
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

        {reviewError && <p className="people-empty">{reviewError}</p>}

        <div className="review-write-section">
          {/* 리뷰 작성 열기 */}
          <button
            className="review-write-toggle"
            onClick={() => setIsReviewOpen((prev) => !prev)}
          >
            <span>{movie.title} 리뷰 작성하기</span>

            <span className="review-toggle-arrow">
              {isReviewOpen ? "▲" : "▼"}
            </span>
          </button>

          {/* 리뷰 작성 영역 */}
          {isReviewOpen && (
            <div className="review-write-content">
              <h3>이 영화는 어떠셨나요?</h3>

              {/* 리뷰 제목 */}
              <div className="review-title-field">
                <label htmlFor="reviewTitle">리뷰 제목</label>

                <input
                  id="reviewTitle"
                  type="text"
                  value={reviewTitle}
                  onChange={(e) => setReviewTitle(e.target.value)}
                  placeholder="리뷰 제목을 입력해 주세요."
                  maxLength={100}
                />

                <span>{reviewTitle.length}/100</span>
              </div>

              {/* 리뷰 내용 */}
              <div className="review-content-field">
                <label htmlFor="reviewContent">리뷰 내용</label>

                <textarea
                  id="reviewContent"
                  value={reviewText}
                  onChange={(e) => setReviewText(e.target.value)}
                  placeholder="영화에 대한 리뷰를 작성해 주세요."
                  maxLength={2000}
                />

                <span>{reviewText.length}/2000</span>
              </div>

              {/* 리뷰 등록 */}
              <div className="review-form-actions">
                <button
                  type="button"
                  onClick={addReview}
                  disabled={isReviewSubmitting}
                >
                  {isReviewSubmitting ? "등록 중..." : "리뷰 등록"}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* 리뷰 목록 */}
        {reviews.length > 0 ? (
          <div className="detail-review-list">
            {reviews.map((review) => (
              <article className="detail-review-card" key={review.reviewId}>
                <div>
                  <strong>{review.title}</strong>

                  <span>
                    {new Date(review.createdAt).toLocaleDateString("ko-KR")}
                  </span>
                </div>

                <p>{review.content}</p>
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
