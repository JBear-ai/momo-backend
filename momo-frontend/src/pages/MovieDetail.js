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
        personId: null,
        name,
        role: "",
      }));
  }

  if (!Array.isArray(people)) {
    people = [people];
  }

  return people.map((person) => {
    if (typeof person === "string") {
      return {
        personId: null,
        name: person,
        role: "",
      };
    }

    return {
      personId:
        person.personId || person.tmdbPersonId || person.peopleId || null,
      name:
        person.name ||
        person.peopleNm ||
        person.actorName ||
        person.directorName ||
        "이름 없음",
      role:
        person.role || person.cast || person.character || person.castName || "",
    };
  });
}

function MovieDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  // =========================================================
  // 1. 영화 상세정보 상태
  // =========================================================

  const [movie, setMovie] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // =========================================================
  // 2. 영화 평가 상태
  // =========================================================

  const [ratingSummary, setRatingSummary] = useState({
    likeCount: 0,
    dislikeCount: 0,
    totalCount: 0,
    positiveRate: null,
    myRating: null,
  });

  const [isRatingSubmitting, setIsRatingSubmitting] = useState(false);
  const [ratingError, setRatingError] = useState("");

  // =========================================================
  // 3. 리뷰 상태
  // =========================================================

  const [reviews, setReviews] = useState([]);

  const [reviewTitle, setReviewTitle] = useState("");
  const [reviewText, setReviewText] = useState("");

  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const [isReviewSubmitting, setIsReviewSubmitting] = useState(false);

  const [reviewError, setReviewError] = useState("");

  // =========================================================
  // 4. 영화 상세정보 및 포스터 조회
  // =========================================================

  useEffect(() => {
    async function fetchMovieDetail() {
      try {
        setLoading(true);
        setError("");

        const detailResponse = await fetch(
          `http://localhost:8111/api/movies/${id}`,
        );

        if (!detailResponse.ok) {
          throw new Error(`영화 상세정보 조회 실패: ${detailResponse.status}`);
        }

        const detailData = await detailResponse.json();

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

  // =========================================================
  // 5. 영화별 리뷰 및 평가 조회
  // =========================================================

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

        setRatingSummary({
          likeCount: ratingResponse.data.likeCount || 0,
          dislikeCount: ratingResponse.data.dislikeCount || 0,
          totalCount: ratingResponse.data.totalCount || 0,
          positiveRate: ratingResponse.data.positiveRate ?? null,
          myRating: ratingResponse.data.myRating ?? null,
        });
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

  // =========================================================
  // 6. 영화 평가 데이터
  // =========================================================

  const likeCount = ratingSummary.likeCount || 0;
  const dislikeCount = ratingSummary.dislikeCount || 0;
  const totalCount = ratingSummary.totalCount || 0;
  const myRating = ratingSummary.myRating;

  // =========================================================
  // 7. 영화 평가 갱신
  // =========================================================

  async function refreshMovieRating() {
    const response = await AxiosApi.movieRating(movie.movieCode);

    setRatingSummary({
      likeCount: response.data.likeCount || 0,
      dislikeCount: response.data.dislikeCount || 0,
      totalCount: response.data.totalCount || 0,
      positiveRate: response.data.positiveRate ?? null,
      myRating: response.data.myRating ?? null,
    });
  }

  // =========================================================
  // 8. 영화 좋아요 / 싫어요 선택 및 취소
  // =========================================================

  async function handleMovieRating(liked) {
    if (isRatingSubmitting) return;

    const previousSummary = { ...ratingSummary };
    const previousRating = previousSummary.myRating;
    const isCancel = previousRating === liked;

    // 버튼 클릭 즉시 화면에 카운트 반영
    setRatingSummary((prev) => {
      let nextLikeCount = prev.likeCount;
      let nextDislikeCount = prev.dislikeCount;

      // 기존 평가 제거
      if (previousRating === true) {
        nextLikeCount--;
      } else if (previousRating === false) {
        nextDislikeCount--;
      }

      // 취소가 아니라면 새로운 평가 추가
      if (!isCancel) {
        if (liked) {
          nextLikeCount++;
        } else {
          nextDislikeCount++;
        }
      }

      nextLikeCount = Math.max(0, nextLikeCount);
      nextDislikeCount = Math.max(0, nextDislikeCount);

      const nextTotalCount = nextLikeCount + nextDislikeCount;

      return {
        ...prev,
        likeCount: nextLikeCount,
        dislikeCount: nextDislikeCount,
        totalCount: nextTotalCount,
        positiveRate:
          nextTotalCount > 0
            ? Math.round((nextLikeCount / nextTotalCount) * 100)
            : null,
        myRating: isCancel ? null : liked,
      };
    });

    try {
      setIsRatingSubmitting(true);
      setRatingError("");

      if (isCancel) {
        await AxiosApi.movieRatingDelete(movie.movieCode);
      } else {
        await AxiosApi.movieRatingSave(movie.movieCode, liked);
      }

      await refreshMovieRating();
    } catch (error) {
      console.error("영화 평가 처리 실패:", error);

      // 서버 요청 실패 시 기존 평가 상태 복구
      setRatingSummary(previousSummary);

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

  // =========================================================
  // 9. 리뷰 등록
  // =========================================================

  async function addReview() {
    if (!reviewTitle.trim()) {
      alert("리뷰 제목을 입력해 주세요.");
      return;
    }

    if (reviewTitle.trim().length > 100) {
      alert("리뷰 제목은 100자 이하로 입력해 주세요.");
      return;
    }

    if (!reviewText.trim()) {
      alert("리뷰 내용을 입력해 주세요.");
      return;
    }

    if (reviewText.trim().length > 2000) {
      alert("리뷰 내용은 2000자 이하로 입력해 주세요.");
      return;
    }

    try {
      setIsReviewSubmitting(true);
      setReviewError("");

      const createResponse = await AxiosApi.reviewCreate(
        movie.movieCode,
        reviewTitle.trim(),
        reviewText.trim(),
      );

      setReviews((prev) => [createResponse.data, ...prev]);

      setReviewTitle("");
      setReviewText("");
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

  // =========================================================
  // 10. 감독 및 배우 인물 상세 페이지 이동
  // =========================================================

  function moveToPersonDetail(person, type) {
    const identifier = person.personId || person.name;

    navigate(`/person/${type}/${encodeURIComponent(identifier)}`);
  }

  // =========================================================
  // 11. 영화 정보 로딩 중
  // =========================================================

  if (loading) {
    return (
      <main className="movie-detail-page">
        <p>영화 정보를 불러오는 중입니다.</p>
      </main>
    );
  }

  // =========================================================
  // 12. 영화 정보 조회 실패
  // =========================================================

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

  // =========================================================
  // 13. 감독과 배우 데이터 변환
  // =========================================================

  const directors = normalizePeople(movie.director);
  const actors = normalizePeople(movie.actors);

  // =========================================================
  // 14. 화면 출력
  // =========================================================

  return (
    <main className="movie-detail-page">
      <button className="detail-back-button" onClick={() => navigate("/")}>
        ← 메인으로 돌아가기
      </button>

      {/* 영화 기본정보 */}
      <section className="movie-detail-header">
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

        <div className="movie-detail-info">
          <span className="detail-rank">영화 상세정보</span>

          <h1>{movie.title}</h1>

          <p>영문 제목: {movie.titleEn || "정보 없음"}</p>

          <p>개봉일: {formatReleaseDate(movie.releaseDate)}</p>

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
        <h2 className="rating-label">관객 평점</h2>

        <div className="detail-reaction-counts">
          <button
            type="button"
            className={`detail-reaction-count ${
              myRating === true ? "selected-like" : ""
            }`}
            onClick={() => handleMovieRating(true)}
            disabled={isRatingSubmitting}
            aria-pressed={myRating === true}
          >
            <span>👍 좋아요</span>

            <strong>{formatNumber(likeCount)}</strong>
          </button>

          <button
            type="button"
            className={`detail-reaction-count ${
              myRating === false ? "selected-dislike" : ""
            }`}
            onClick={() => handleMovieRating(false)}
            disabled={isRatingSubmitting}
            aria-pressed={myRating === false}
          >
            <span>👎 싫어요</span>

            <strong>{formatNumber(dislikeCount)}</strong>
          </button>
        </div>

        <p className="detail-total-count">
          전체 평가 수: <strong>{formatNumber(totalCount)}</strong>
        </p>

        {isRatingSubmitting && (
          <p className="rating-status">평가를 저장하고 있습니다.</p>
        )}

        {ratingError && <p className="people-empty">{ratingError}</p>}
      </section>

      {/* 감독 */}
      <section className="detail-content-section">
        <h2>감독</h2>

        {directors.length > 0 ? (
          <div className="people-list">
            {directors.map((person, index) => (
              <button
                type="button"
                className="person-chip"
                key={`${person.name}-${index}`}
                onClick={() => moveToPersonDetail(person, "director")}
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
                type="button"
                className="person-chip"
                key={`${person.name}-${index}`}
                onClick={() => moveToPersonDetail(person, "actor")}
              >
                {person.name}

                {person.role && (
                  <span className="person-role"> ({person.role})</span>
                )}
              </button>
            ))}
          </div>
        ) : (
          <p className="people-empty">배우 정보가 아직 연결되지 않았습니다.</p>
        )}
      </section>

      {/* 리뷰 */}
      <section className="detail-content-section">
        <h2>리뷰</h2>

        {reviewError && <p className="people-empty">{reviewError}</p>}

        <div className="review-write-section">
          <button
            type="button"
            className="review-write-toggle"
            onClick={() => setIsReviewOpen((prev) => !prev)}
          >
            <span>{movie.title} 리뷰 작성하기</span>

            <span className="review-toggle-arrow">
              {isReviewOpen ? "▲" : "▼"}
            </span>
          </button>

          {isReviewOpen && (
            <div className="review-write-content">
              <h3>이 영화는 어떠셨나요?</h3>

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
                    {review.createdAt
                      ? new Date(review.createdAt).toLocaleDateString("ko-KR")
                      : ""}
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
