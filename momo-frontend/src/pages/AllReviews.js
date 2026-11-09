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

  // 영화 정보 및 리뷰 통계
  const [reviewInfo, setReviewInfo] = useState({});

  // 전체 리뷰 조회
  useEffect(() => {
    const controller = new AbortController();

    const fetchReviews = async () => {
      try {
        setLoading(true);
        setError("");
        setReviewInfo({});

        const response = await AxiosApi.allReviews(page, controller.signal);

        if (controller.signal.aborted) return;

        const data = response.data;
        const reviewList = data.content || [];

        setReviews(reviewList);
        setTotalPages(data.totalPages || 0);

        // 리뷰별 포스터, 댓글 수, 좋아요 및 싫어요 수 조회
        const results = await Promise.all(
          reviewList.map(async (review) => {
            const [posterResult, commentsResult, reactionResult] =
              await Promise.allSettled([
                review.movieCode
                  ? AxiosApi.moviePoster(review.movieCode, controller.signal)
                  : Promise.resolve(null),

                AxiosApi.reviewComments(review.reviewId, 0, controller.signal),

                AxiosApi.reviewReactionCounts(
                  review.reviewId,
                  controller.signal,
                ),
              ]);

            if (controller.signal.aborted) return null;

            const poster =
              posterResult.status === "fulfilled" && posterResult.value
                ? posterResult.value.data
                : null;

            const comments =
              commentsResult.status === "fulfilled"
                ? commentsResult.value.data
                : null;

            const reactions =
              reactionResult.status === "fulfilled"
                ? reactionResult.value.data
                : null;

            if (posterResult.status === "rejected") {
              console.error("영화 포스터 조회 실패:", posterResult.reason);
            }

            if (commentsResult.status === "rejected") {
              console.error("댓글 수 조회 실패:", commentsResult.reason);
            }

            if (reactionResult.status === "rejected") {
              console.error("리뷰 반응 개수 조회 실패:", reactionResult.reason);
            }

            return {
              reviewId: review.reviewId,
              posterUrl: poster?.posterUrl || "",
              commentCount:
                comments?.totalElements ?? comments?.content?.length ?? 0,
              likeCount: reactions?.likeCount ?? 0,
              dislikeCount: reactions?.dislikeCount ?? 0,
            };
          }),
        );

        if (controller.signal.aborted) return;

        const infoMap = {};

        results.forEach((result) => {
          if (result) {
            infoMap[result.reviewId] = result;
          }
        });

        setReviewInfo(infoMap);
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
                    <th className="review-stats-column">반응</th>
                  </tr>
                </thead>

                <tbody>
                  {reviews.map((review) => {
                    const info = reviewInfo[review.reviewId];

                    return (
                      <tr
                        key={review.reviewId}
                        className="review-clickable-row"
                        onClick={() => handleReviewClick(review.reviewId)}
                      >
                        <td className="review-movie-cell">
                          <div className="all-review-movie-info">
                            <div className="all-review-poster">
                              {info?.posterUrl ? (
                                <img
                                  src={info.posterUrl}
                                  alt={review.movieTitle || "영화 포스터"}
                                />
                              ) : (
                                <div className="all-review-poster-placeholder">
                                  MOMO
                                </div>
                              )}
                            </div>

                            <div>
                              <span className="review-movie-title">
                                {review.movieTitle || "알 수 없는 영화"}
                              </span>

                              {review.movieYear && (
                                <span className="review-movie-year">
                                  ({review.movieYear})
                                </span>
                              )}
                            </div>
                          </div>
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

                        <td className="review-stats-cell">
                          <div className="all-review-stats">
                            <span>댓글 {info?.commentCount ?? 0}</span>
                            <span>좋아요 {info?.likeCount ?? 0}</span>
                            <span>싫어요 {info?.dislikeCount ?? 0}</span>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* 모바일 목록 */}
            <div className="all-reviews-mobile-list">
              {reviews.map((review) => {
                const info = reviewInfo[review.reviewId];

                return (
                  <article
                    className="all-reviews-mobile-item"
                    key={review.reviewId}
                    onClick={() => handleReviewClick(review.reviewId)}
                  >
                    <div className="all-review-mobile-content">
                      <div className="all-review-poster">
                        {info?.posterUrl ? (
                          <img
                            src={info.posterUrl}
                            alt={review.movieTitle || "영화 포스터"}
                          />
                        ) : (
                          <div className="all-review-poster-placeholder">
                            MOMO
                          </div>
                        )}
                      </div>

                      <div className="all-review-mobile-info">
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
                      </div>
                    </div>

                    <div className="all-review-mobile-stats">
                      <span>
                        댓글 <strong>{info?.commentCount ?? 0}</strong>
                      </span>
                      <span>
                        좋아요 <strong>{info?.likeCount ?? 0}</strong>
                      </span>
                      <span>
                        싫어요 <strong>{info?.dislikeCount ?? 0}</strong>
                      </span>
                    </div>
                  </article>
                );
              })}
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
