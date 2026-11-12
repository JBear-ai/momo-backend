import SafeImage from "../components/common/SafeImage";
import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";
import { resolvePosterUrl } from "../utils/mediaUrl";

function LikedReviews() {
  const [reviews, setReviews] = useState([]);
  const [reviewInfo, setReviewInfo] = useState({});

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [removingId, setRemovingId] = useState(null);

  // Axios 요청 취소 여부 확인
  const isCanceled = (err) =>
    err?.name === "CanceledError" || err?.code === "ERR_CANCELED";

  // 좋아요한 리뷰 목록 및 관련 정보 조회
  useEffect(() => {
    const controller = new AbortController();

    const fetchLikedReviews = async () => {
      try {
        setLoading(true);
        setError("");
        setReviewInfo({});

        // 로그인 회원이 좋아요한 리뷰 조회
        const response = await AxiosApi.myLikedReviews(page, controller.signal);

        if (controller.signal.aborted) return;

        const data = response.data ?? {};
        const reviewList = Array.isArray(data.content) ? data.content : [];

        // Spring Data PageSerializationMode.VIA_DTO 응답 구조 지원
        setReviews(reviewList);
        setTotalPages(data.page?.totalPages ?? data.totalPages ?? 0);
        setTotalElements(data.page?.totalElements ?? data.totalElements ?? 0);

        // 좋아요한 리뷰의 영화 정보·통계·포스터 수집을 페이지 단위로 조회한다.
        const reviewIds = reviewList.map((review) => review.reviewId);
        const movieCodes = [...new Set(
          reviewList.map((review) => review.movieCode).filter(Boolean),
        )];

        const [summaryResult, posterResult] = await Promise.all([
          reviewIds.length
            ? AxiosApi.reviewSummaries(reviewIds, controller.signal)
                .then((result) => ({ status: "fulfilled", data: result.data }))
                .catch((reason) => ({ status: "rejected", reason }))
            : Promise.resolve({ status: "fulfilled", data: [] }),
          movieCodes.length
            ? AxiosApi.moviePostersEnsure(movieCodes, controller.signal)
                .then((result) => ({ status: "fulfilled", data: result.data }))
                .catch((reason) => ({ status: "rejected", reason }))
            : Promise.resolve({ status: "fulfilled", data: [] }),
        ]);

        if (controller.signal.aborted) return;

        if (summaryResult.status === "rejected") {
          console.error("리뷰 요약 조회 실패:", summaryResult.reason);
        }
        if (posterResult.status === "rejected") {
          console.error("일괄 포스터 조회 실패:", posterResult.reason);
        }

        const summaryByReviewId = new Map(
          (summaryResult.data || []).map((summary) => [summary.reviewId, summary]),
        );
        const posterByMovieCode = new Map(
          (posterResult.data || []).map((poster) => [poster.movieCode, poster]),
        );
        const infoMap = {};

        reviewList.forEach((review) => {
          const summary = summaryByReviewId.get(review.reviewId);
          const poster = posterByMovieCode.get(review.movieCode);
          infoMap[review.reviewId] = {
            title: summary?.movieTitle || "영화 정보 없음",
            releaseDate: summary?.releaseDate || "",
            posterUrl: poster?.posterUrl || "",
            commentCount: summary?.commentCount ?? 0,
            likeCount: summary?.likeCount ?? 0,
            dislikeCount: summary?.dislikeCount ?? 0,
          };
        });

        setReviewInfo(infoMap);
      } catch (err) {
        if (isCanceled(err)) return;

        console.error("좋아요한 리뷰 조회 실패:", err);
        setError("좋아요한 리뷰를 불러오지 못했습니다.");
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    fetchLikedReviews();

    return () => {
      controller.abort();
    };
  }, [page, refreshKey]);

  // 날짜 변환
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

  // 개봉 연도
  const getReleaseYear = (date) => {
    return date ? String(date).substring(0, 4) : "";
  };

  // 좋아요 취소
  const handleUnlike = async (reviewId) => {
    if (removingId !== null) return;

    const confirmed = window.confirm("이 리뷰의 좋아요를 취소하시겠습니까?");

    if (!confirmed) return;

    try {
      setRemovingId(reviewId);

      // 백엔드에서 로그인 회원의 해당 리뷰 반응 삭제
      await AxiosApi.reviewReactionDelete(reviewId);

      // 좋아요 취소 후 전체 개수 갱신
      setTotalElements((prev) => Math.max(0, prev - 1));

      // 현재 페이지에 리뷰가 하나뿐이고 첫 페이지가 아니라면
      // 이전 페이지로 이동
      if (reviews.length === 1 && page > 0) {
        setPage((prev) => prev - 1);
      } else {
        // 현재 페이지 다시 조회
        setRefreshKey((prev) => prev + 1);
      }
    } catch (err) {
      if (isCanceled(err)) return;

      console.error("좋아요 취소 실패:", err);

      alert(err.response?.data?.message || "좋아요 취소에 실패했습니다.");
    } finally {
      setRemovingId(null);
    }
  };

  // 페이지 이동
  const handlePageChange = (nextPage) => {
    if (nextPage < 0 || nextPage >= totalPages || nextPage === page) {
      return;
    }

    setPage(nextPage);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  return (
    <main className="liked-reviews-page">
      <div className="liked-reviews-container">
        {/* 페이지 제목 */}
        <header className="liked-reviews-heading">
          <div>
            <span className="liked-reviews-eyebrow">MY MOMO</span>

            <h1>좋아요한 리뷰</h1>

            <p>내가 마음에 들었던 영화 리뷰를 모아볼 수 있어요.</p>
          </div>

          <Link to="/profile">← 마이페이지</Link>
        </header>

        {/* 리뷰 목록 */}
        <section className="liked-reviews-card">
          <div className="liked-reviews-toolbar">
            <div>
              <h2>좋아요 리뷰 모아보기</h2>

              <p>내가 좋아요를 누른 리뷰를 최신순으로 보여드립니다.</p>
            </div>

            <span>{totalElements}개</span>
          </div>

          {/* 로딩 */}
          {loading ? (
            <div className="liked-reviews-state">
              리뷰를 불러오는 중입니다...
            </div>
          ) : /* 오류 */
          error ? (
            <div className="liked-reviews-state error">{error}</div>
          ) : /* 좋아요한 리뷰 없음 */
          reviews.length === 0 ? (
            <div className="liked-reviews-state">
              <span className="liked-empty-icon">♥</span>

              <h3>아직 좋아요한 리뷰가 없습니다.</h3>

              <p>전체 리뷰에서 마음에 드는 리뷰에 좋아요를 눌러보세요.</p>

              <Link to="/reviews">전체 리뷰 보러 가기 →</Link>
            </div>
          ) : (
            /* 리뷰 목록 */
            <div className="liked-reviews-list">
              {reviews.map((review) => {
                const info = reviewInfo[review.reviewId];

                return (
                  <article className="liked-review-item" key={review.reviewId}>
                    {/* 영화 포스터 */}
                    <div className="liked-review-poster">
                      <SafeImage
                        src={resolvePosterUrl(info?.posterUrl)}
                        alt={info?.title || "영화 포스터"}
                        fallbackClassName="liked-poster-placeholder"
                      />
                    </div>

                    {/* 리뷰 정보 */}
                    <div className="liked-review-info">
                      <span className="liked-review-movie">
                        {info?.title || "영화 정보 불러오는 중"}

                        {info?.releaseDate &&
                          ` · ${getReleaseYear(info.releaseDate)}`}
                      </span>

                      <Link
                        to={`/reviews/${review.reviewId}`}
                        className="liked-review-title"
                      >
                        {review.title}
                      </Link>

                      <p className="liked-review-content">{review.content}</p>

                      <div className="liked-review-meta">
                        <span>작성자 {review.nickname || "익명"}</span>

                        <span>{formatDate(review.createdAt)}</span>
                      </div>

                      <div className="liked-review-stats">
                        <span>
                          ♥ 좋아요 <strong>{info?.likeCount ?? 0}</strong>
                        </span>

                        <span>
                          댓글 <strong>{info?.commentCount ?? 0}</strong>
                        </span>
                      </div>
                    </div>

                    {/* 좋아요 취소 */}
                    <button
                      type="button"
                      className="liked-review-unlike"
                      disabled={removingId === review.reviewId}
                      onClick={() => handleUnlike(review.reviewId)}
                    >
                      {removingId === review.reviewId
                        ? "취소 중..."
                        : "♥ 좋아요 취소"}
                    </button>
                  </article>
                );
              })}
            </div>
          )}

          {/* 페이지네이션 */}
          {!loading && !error && totalPages > 1 && (
            <div className="liked-reviews-pagination">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => handlePageChange(page - 1)}
              >
                이전
              </button>

              {Array.from({ length: totalPages }, (_, index) => index).map(
                (pageNumber) => (
                  <button
                    type="button"
                    key={pageNumber}
                    className={pageNumber === page ? "active" : ""}
                    onClick={() => handlePageChange(pageNumber)}
                  >
                    {pageNumber + 1}
                  </button>
                ),
              )}

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
      </div>
    </main>
  );
}

export default LikedReviews;
