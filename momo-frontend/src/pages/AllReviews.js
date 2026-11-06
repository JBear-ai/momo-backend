import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";

function AllReviews() {
  const [reviews, setReviews] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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
        if (err.name === "CanceledError") return;

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

  const handlePageChange = (nextPage) => {
    if (nextPage < 0 || nextPage >= totalPages) return;

    setPage(nextPage);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const pageNumbers = Array.from({ length: totalPages }, (_, index) => index);

  return (
    React.createElement(
  "div",
  {"className":"all-reviews-page"},
  React.createElement(
      "section",
      {"className":"all-reviews-heading"},
      React.createElement(
            "div",
            null,
            React.createElement(
                    "h1",
                    null,
                    "모든 영화 리뷰"
                  ),
            React.createElement(
                    "p",
                    {"className":"all-reviews-description"},
                    "영화에 대한 다양한 생각과 기록을 만나보세요."
                  )
          )
    ),
  React.createElement(
      "section",
      {"className":"all-reviews-card"},
      React.createElement(
            "div",
            {"className":"all-reviews-card-heading"},
            React.createElement(
                    "div",
                    null,
                    React.createElement(
                              "h2",
                              null,
                              "리뷰 모아보기"
                            ),
                    React.createElement(
                              "p",
                              null,
                              "최근 작성된 리뷰부터 보여드립니다."
                            )
                  ),
            React.createElement(
                    "span",
                    {"className":"all-reviews-page-info"},
                    (totalPages > 0 ? `${page + 1} / ${totalPages} 페이지` : "리뷰")
                  )
          ),
      (loading ? (
                React.createElement(
            "div",
            {"className":"all-reviews-state"},
            "리뷰를 불러오는 중입니다."
          )
              ) : error ? (
                React.createElement(
            "div",
            {"className":"all-reviews-state all-reviews-error"},
            (error)
          )
              ) : reviews.length === 0 ? (
                React.createElement(
            "div",
            {"className":"all-reviews-state"},
            "아직 작성된 리뷰가 없습니다."
          )
              ) : (
                React.createElement(
            React.Fragment,
            null,
            React.createElement(
                    "div",
                    {"className":"all-reviews-table-wrap"},
                    React.createElement(
                              "table",
                              {"className":"all-reviews-table"},
                              React.createElement(
                                          "thead",
                                          null,
                                          React.createElement(
                                                        "tr",
                                                        null,
                                                        React.createElement(
                                                                        "th",
                                                                        {"className":"review-movie-column"},
                                                                        "영화"
                                                                      ),
                                                        React.createElement(
                                                                        "th",
                                                                        {"className":"review-title-column"},
                                                                        "리뷰 제목"
                                                                      ),
                                                        React.createElement(
                                                                        "th",
                                                                        {"className":"review-author-column"},
                                                                        "작성자"
                                                                      ),
                                                        React.createElement(
                                                                        "th",
                                                                        {"className":"review-date-column"},
                                                                        "작성일"
                                                                      )
                                                      )
                                        ),
                              React.createElement(
                                          "tbody",
                                          null,
                                          (reviews.map((review) => (
                                                              React.createElement(
                                                        "tr",
                                                        {"key":review.reviewId},
                                                        React.createElement(
                                                                        "td",
                                                                        {"className":"review-movie-cell"},
                                                                        React.createElement(
                                                                                          "span",
                                                                                          {"className":"review-movie-title"},
                                                                                          (review.movieTitle || "알 수 없는 영화")
                                                                                        ),
                                                                        (review.movieYear && (
                                                                                                  React.createElement(
                                                                                          "span",
                                                                                          {"className":"review-movie-year"},
                                                                                          "(",
                                                                                          (review.movieYear),
                                                                                          ")"
                                                                                        )
                                                                                                ))
                                                                      ),
                                                        React.createElement(
                                                                        "td",
                                                                        {"className":"review-title-cell"},
                                                                        React.createElement(
                                                                                          Link,
                                                                                          {"to":`/reviews/${review.reviewId}`,"className":"review-title-link"},
                                                                                          (review.title)
                                                                                        )
                                                                      ),
                                                        React.createElement(
                                                                        "td",
                                                                        {"className":"review-author-cell"},
                                                                        (review.nickname || "익명")
                                                                      ),
                                                        React.createElement(
                                                                        "td",
                                                                        {"className":"review-date-cell"},
                                                                        (formatDate(review.createdAt))
                                                                      )
                                                      )
                                                            )))
                                        )
                            )
                  ),
            React.createElement(
                    "div",
                    {"className":"all-reviews-mobile-list"},
                    (reviews.map((review) => (
                                    React.createElement(
                              "article",
                              {"className":"all-reviews-mobile-item","key":review.reviewId},
                              React.createElement(
                                          "p",
                                          {"className":"mobile-review-movie"},
                                          (review.movieTitle || "알 수 없는 영화"),
                                          (review.movieYear ? ` (${review.movieYear})` : "")
                                        ),
                              React.createElement(
                                          Link,
                                          {"to":`/reviews/${review.reviewId}`,"className":"mobile-review-title"},
                                          (review.title)
                                        ),
                              React.createElement(
                                          "div",
                                          {"className":"mobile-review-meta"},
                                          React.createElement(
                                                        "span",
                                                        null,
                                                        (review.nickname || "익명")
                                                      ),
                                          React.createElement(
                                                        "span",
                                                        null,
                                                        (formatDate(review.createdAt))
                                                      )
                                        )
                            )
                                  )))
                  ),
            React.createElement(
                    "div",
                    {"className":"all-reviews-pagination"},
                    React.createElement(
                              "button",
                              {"type":"button","onClick":() => handlePageChange(page - 1),"disabled":page === 0},
                              "이전"
                            ),
                    (pageNumbers.map((pageNumber) => (
                                    React.createElement(
                              "button",
                              {"type":"button","key":pageNumber,"className":pageNumber === page ? "active" : "","onClick":() => handlePageChange(pageNumber)},
                              (pageNumber + 1)
                            )
                                  ))),
                    React.createElement(
                              "button",
                              {"type":"button","onClick":() => handlePageChange(page + 1),"disabled":page >= totalPages - 1},
                              "다음"
                            )
                  )
          )
              ))
    )
)
  );
}

export default AllReviews;
