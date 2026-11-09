import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";

function AllReviews() {
  const navigate = useNavigate();

  const [reviews, setReviews] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // 전체 리뷰 조회
  useEffect(() => {
    const controller = new AbortController();

    const fetchReviews = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await AxiosApi.allReviews(page, controller.signal);

        const data = response.data;

        setReviews(data.content || []);
        setTotalPages(data.totalPages || 0);
      } catch (err) {
        if (err.name === "CanceledError" || err.code === "ERR_CANCELED") {
          return;
        }

        console.error("전체 리뷰 조회 실패:", err);
        setError("리뷰 목록을 불러오지 못했습니다.");
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchReviews();

    return () => controller.abort();
  }, [page]);

  // 날짜 형식 변환
  const formatDate = (dateString) => {
    if (!dateString) return "-";

    const date = new Date(dateString);

    if (Number.isNaN(date.getTime())) return "-";

    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  };

  // 리뷰 상세 페이지 이동
  const handleReviewClick = (reviewId) => {
    navigate(`/reviews/${reviewId}`);
  };

  // 페이지 변경
  const handlePageChange = (nextPage) => {
    if (nextPage < 0 || nextPage >= totalPages) return;

    setPage(nextPage);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  // 페이지 번호 생성
  const pageNumbers = Array.from({ length: totalPages }, (_, index) => index);

  return (
    <div className="all-reviews-page">
      {/* 상단 소개 */}
      <section className="all-reviews-heading">
        <div>
          <h1>모든 영화 리뷰</h1>

          <p className="all-reviews-description">
            영화에 대한 다양한 생각과 기록을 만나보세요.
          </p>
        </div>
      </section>

      {/* 리뷰 목록 */}
      <section className="all-reviews-card">
        <div className="all-reviews-card-heading">
          <div>
            <h2>리뷰 모아보기</h2>

            <p>최근 작성된 리뷰부터 보여드립니다.</p>
          </div>

          <span className="all-reviews-page-info">
            {totalPages > 0 ? `${page + 1} / ${totalPages} 페이지` : "리뷰"}
          </span>
        </div>

        {loading ? (
          <div className="all-reviews-state">리뷰를 불러오는 중입니다.</div>
        ) : error ? (
          <div className="all-reviews-state all-reviews-error">{error}</div>
        ) : reviews.length === 0 ? (
          <div className="all-reviews-state">아직 작성된 리뷰가 없습니다.</div>
        ) : (
          <>
            {/* PC 테이블 */}
            <div className="all-reviews-table-wrap">
              <table className="all-reviews-table">
                <thead>
                  <tr>
                    <th className="review-movie-column">영화</th>
                    <th className="review-title-column">리뷰 제목</th>
                    <th className="review-author-column">작성자</th>
                    <th className="review-date-column">작성일</th>
                  </tr>
                </thead>

                <tbody>
                  {reviews.map((review) => (
                    <tr
                      key={review.reviewId}
                      className="review-clickable-row"
                      onClick={() => handleReviewClick(review.reviewId)}
                    >
                      <td className="review-movie-cell">
                        <span className="review-movie-title">
                          {review.movieTitle || "알 수 없는 영화"}
                        </span>

                        {review.movieYear && (
                          <span className="review-movie-year">
                            ({review.movieYear})
                          </span>
                        )}
                      </td>

                      <td className="review-title-cell">
                        <Link
                          to={`/reviews/${review.reviewId}`}
                          className="review-title-link"
                          onClick={(event) => event.stopPropagation()}
                        >
                          {review.title}
                        </Link>
                      </td>

                      <td className="review-author-cell">
                        {review.nickname || "익명"}
                      </td>

                      <td className="review-date-cell">
                        {formatDate(review.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 모바일 목록 */}
            <div className="all-reviews-mobile-list">
              {reviews.map((review) => (
                <article
                  className="all-reviews-mobile-item"
                  key={review.reviewId}
                  onClick={() => handleReviewClick(review.reviewId)}
                >
                  <p className="mobile-review-movie">
                    {review.movieTitle || "알 수 없는 영화"}
                    {review.movieYear ? ` (${review.movieYear})` : ""}
                  </p>

                  <Link
                    to={`/reviews/${review.reviewId}`}
                    className="mobile-review-title"
                    onClick={(event) => event.stopPropagation()}
                  >
                    {review.title}
                  </Link>

                  <div className="mobile-review-meta">
                    <span>{review.nickname || "익명"}</span>
                    <span>{formatDate(review.createdAt)}</span>
                  </div>
                </article>
              ))}
            </div>

            {/* 페이지네이션 */}
            <div className="all-reviews-pagination">
              <button
                type="button"
                onClick={() => handlePageChange(page - 1)}
                disabled={page === 0}
              >
                이전
              </button>

              {pageNumbers.map((pageNumber) => (
                <button
                  type="button"
                  key={pageNumber}
                  className={pageNumber === page ? "active" : ""}
                  onClick={() => handlePageChange(pageNumber)}
                >
                  {pageNumber + 1}
                </button>
              ))}

              <button
                type="button"
                onClick={() => handlePageChange(page + 1)}
                disabled={page >= totalPages - 1}
              >
                다음
              </button>
            </div>
          </>
        )}
      </section>
    </div>
  );
}

export default AllReviews;
