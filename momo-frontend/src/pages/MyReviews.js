import SafeImage from "../components/common/SafeImage";
import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";
import { resolvePosterUrl } from "../utils/mediaUrl";

function MyReviews() {
  const [reviews, setReviews] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // 영화 정보 및 리뷰 통계
  const [reviewInfo, setReviewInfo] = useState({});

  useEffect(() => {
    const controller = new AbortController();

    const fetchMyReviews = async () => {
      try {
        setLoading(true);
        setError("");
        setReviewInfo({});

        const response = await AxiosApi.myReviews(page, controller.signal);

        if (controller.signal.aborted) return;

        const data = response.data;
        const reviewList = data.content || [];

        setReviews(reviewList);
        setTotalPages(data.page?.totalPages ?? data.totalPages ?? 0);

        // 리뷰별 영화 정보, 댓글 수, 좋아요 및 싫어요 수 조회
        const results = await Promise.all(
          reviewList.map(async (review) => {
            const movieCode = review.movieCode;

            const [detailResult, posterResult, commentsResult, reactionResult] =
              await Promise.allSettled([
                movieCode
                  ? AxiosApi.movieDetail(movieCode, controller.signal)
                  : Promise.resolve(null),

                movieCode
                  ? AxiosApi.moviePoster(movieCode, controller.signal)
                  : Promise.resolve(null),

                AxiosApi.reviewComments(review.reviewId, 0, controller.signal),

                AxiosApi.reviewReactionCounts(
                  review.reviewId,
                  controller.signal,
                ),
              ]);

            if (controller.signal.aborted) return null;

            const movie =
              detailResult.status === "fulfilled" && detailResult.value
                ? detailResult.value.data
                : null;

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

            if (detailResult.status === "rejected") {
              console.error("영화 정보 조회 실패:", detailResult.reason);
            }

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
              title: movie?.title || "",
              releaseDate: movie?.releaseDate || "",
              posterUrl: poster?.posterUrl || "",
              commentCount:
                comments?.page?.totalElements ?? comments?.totalElements ?? comments?.content?.length ?? 0,
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

        console.error("내 리뷰 조회 실패:", err);
        setError("내 리뷰를 불러오지 못했습니다.");
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchMyReviews();

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

  // 개봉 연도 추출
  const getReleaseYear = (releaseDate) => {
    if (!releaseDate) return "";

    return String(releaseDate).substring(0, 4);
  };

  // 페이지 이동
  const handlePageChange = (nextPage) => {
    if (nextPage < 0 || nextPage >= totalPages) return;

    setPage(nextPage);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const pageNumbers = Array.from({ length: totalPages }, (_, index) => index);

  return (
    <main className="my-reviews-page">
      <section className="my-reviews-heading">
        <div>
          <p className="my-reviews-eyebrow">MY MOMO</p>

          <h1>내가 작성한 리뷰</h1>

          <p>내가 남긴 영화 감상 기록을 모아볼 수 있어요.</p>
        </div>

        <Link to="/profile">← 마이페이지</Link>
      </section>

      <section className="my-reviews-card">
        <div className="my-reviews-card-heading">
          <div>
            <h2>리뷰 모아보기</h2>

            <p>내가 작성한 리뷰를 최신순으로 보여드립니다.</p>
          </div>

          <span>{reviews.length}개</span>
        </div>

        {loading ? (
          <div className="my-reviews-state">리뷰를 불러오는 중입니다.</div>
        ) : error ? (
          <div className="my-reviews-state error">{error}</div>
        ) : reviews.length === 0 ? (
          <div className="my-reviews-state">
            <p>아직 작성한 리뷰가 없습니다.</p>

            <Link to="/">영화 보러 가기 →</Link>
          </div>
        ) : (
          <div className="my-reviews-list">
            {reviews.map((review) => {
              const info = reviewInfo[review.reviewId];

              return (
                <Link
                  to={`/reviews/${review.reviewId}`}
                  className="my-review-item"
                  key={review.reviewId}
                >
                  <div className="my-review-poster">
                    <SafeImage
                      src={resolvePosterUrl(info?.posterUrl)}
                      alt={info?.title || "영화 포스터"}
                      fallbackClassName="my-review-poster-placeholder"
                    />
                  </div>

                  <div className="my-review-info">
                    <span className="my-review-movie">
                      {info?.title || "영화 정보 없음"}
                      {getReleaseYear(info?.releaseDate) &&
                        ` · ${getReleaseYear(info.releaseDate)}`}
                    </span>

                    <h3>{review.title}</h3>

                    <p>{review.content}</p>

                    <span className="my-review-date">
                      작성일 {formatDate(review.createdAt)}
                    </span>

                    <div className="my-review-stats">
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
                  </div>

                  <span className="my-review-arrow">→</span>
                </Link>
              );
            })}
          </div>
        )}

        {!loading && !error && totalPages > 1 && (
          <div className="my-reviews-pagination">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => handlePageChange(page - 1)}
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
              disabled={page >= totalPages - 1}
              onClick={() => handlePageChange(page + 1)}
            >
              다음
            </button>
          </div>
        )}
      </section>
    </main>
  );
}

export default MyReviews;
