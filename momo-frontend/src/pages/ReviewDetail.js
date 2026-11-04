import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";

function ReviewDetail() {
  const { reviewId } = useParams();
  const navigate = useNavigate();

  const [review, setReview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // 날짜 형식 변환
  const formatDate = (dateString) => {
    if (!dateString) return "-";

    const date = new Date(dateString);

    if (Number.isNaN(date.getTime())) return "-";

    return date.toLocaleString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  // 리뷰 상세 조회
  useEffect(() => {
    const controller = new AbortController();

    const fetchReview = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await AxiosApi.reviewDetail(
          reviewId,
          controller.signal,
        );

        setReview(response.data);
      } catch (err) {
        if (err.name === "CanceledError" || err.code === "ERR_CANCELED") {
          return;
        }

        console.error("리뷰 상세 조회 실패:", err);

        if (err.response?.status === 404) {
          setError("존재하지 않는 리뷰입니다.");
        } else if (err.response?.status === 401) {
          setError("리뷰를 조회할 권한이 없습니다.");
        } else {
          setError("리뷰 정보를 불러오지 못했습니다.");
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchReview();

    return () => controller.abort();
  }, [reviewId]);

  // 로딩 화면
  if (loading) {
    return (
      <div className="review-detail-page">
        <div className="review-detail-state">리뷰를 불러오는 중입니다.</div>
      </div>
    );
  }

  // 오류 화면
  if (error || !review) {
    return (
      <div className="review-detail-page">
        <button
          type="button"
          className="review-detail-back"
          onClick={() => navigate("/reviews")}
        >
          ← 전체 리뷰로 돌아가기
        </button>

        <div className="review-detail-state review-detail-error">
          {error || "리뷰 정보를 찾을 수 없습니다."}
        </div>
      </div>
    );
  }

  return (
    <div className="review-detail-page">
      {/* 전체 리뷰로 돌아가기 */}
      <Link to="/reviews" className="review-detail-back">
        ← 전체 리뷰
      </Link>

      <article className="review-detail-card">
        {/* 상단 제목 */}
        <p className="review-detail-eyebrow">MOMO COMMUNITY REVIEW</p>

        <h1 className="review-detail-title">{review.title}</h1>

        {/* 작성자 정보 */}
        <div className="review-detail-meta">
          <span>
            작성자 <strong>{review.nickname || "익명"}</strong>
          </span>

          <span>작성일 {formatDate(review.createdAt)}</span>
        </div>

        {/* 영화 정보 */}
        <section className="review-detail-movie">
          <div className="review-detail-movie-info">
            <span className="review-detail-movie-label">REVIEW MOVIE</span>

            <strong>영화 코드: {review.movieCode || "-"}</strong>
          </div>

          <span className="review-detail-movie-code">
            REVIEW #{review.reviewId}
          </span>
        </section>

        {/* 작성자의 영화 평가 */}
        <div
          className={`review-detail-reaction ${
            review.liked ? "like" : "dislike"
          }`}
        >
          {review.liked ? "좋아요" : "싫어요"}
        </div>

        {/* 리뷰 본문 */}
        <section>
          <p className="review-detail-eyebrow">REVIEW CONTENT</p>

          <div className="review-detail-content">{review.content}</div>
        </section>

        {/* 수정 일자 */}
        {review.updatedAt &&
          new Date(review.updatedAt).getTime() >
            new Date(review.createdAt).getTime() && (
            <p className="review-detail-updated">
              최종 수정일: {formatDate(review.updatedAt)}
            </p>
          )}
      </article>
    </div>
  );
}

export default ReviewDetail;
