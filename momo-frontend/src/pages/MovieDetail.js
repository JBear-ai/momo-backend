import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";
import { formatReleaseDate } from "../utils/releaseDate";
import { resolvePosterUrl } from "../utils/mediaUrl";

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
    const controller = new AbortController();
    let active = true;

    async function fetchMovieDetail() {
      try {
        setLoading(true);
        setError("");
        setMovie(null);

        const detailResponse = await AxiosApi.movieDetail(
          id,
          controller.signal,
        );
        const detailData = detailResponse.data;

        let posterUrl = "";
        try {
          const posterResponse = await AxiosApi.moviePoster(
            id,
            controller.signal,
          );
          posterUrl = posterResponse.data?.posterUrl || "";
        } catch (posterError) {
          if (posterError.code === "ERR_CANCELED") throw posterError;
          console.warn("포스터 조회 실패:", posterError);
        }

        if (!active) return;
        setMovie({
          ...detailData,
          id: detailData.movieCode,
          posterUrl,
        });
      } catch (requestError) {
        if (!active || requestError.code === "ERR_CANCELED") return;
        console.error("영화 상세정보 조회 실패:", requestError);
        setError(
          requestError.response?.data?.message ||
            requestError.message ||
            "영화 정보를 불러오지 못했습니다.",
        );
      } finally {
        if (active) setLoading(false);
      }
    }

    if (id) fetchMovieDetail();
    else setLoading(false);

    return () => {
      active = false;
      controller.abort();
    };
  }, [id]);

  // =========================================================
  // 5. 영화별 리뷰 및 평가 조회
  // =========================================================

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    async function fetchReviewsAndRating() {
      setReviewError("");
      setRatingError("");
      setReviews([]);
      setRatingSummary({
        likeCount: 0,
        dislikeCount: 0,
        totalCount: 0,
        positiveRate: null,
        myRating: null,
      });

      const [reviewsResult, ratingResult] = await Promise.allSettled([
        AxiosApi.movieReviews(id, 0, controller.signal),
        AxiosApi.movieRating(id, controller.signal),
      ]);

      if (!active) return;

      if (reviewsResult.status === "fulfilled") {
        setReviews(reviewsResult.value.data?.content || []);
      } else if (reviewsResult.reason?.code !== "ERR_CANCELED") {
        console.error("영화 메모 조회 실패:", reviewsResult.reason);
        setReviewError("영화 메모를 불러오지 못했습니다.");
      }

      if (ratingResult.status === "fulfilled") {
        const data = ratingResult.value.data || {};
        setRatingSummary({
          likeCount: data.likeCount || 0,
          dislikeCount: data.dislikeCount || 0,
          totalCount: data.totalCount || 0,
          positiveRate: data.positiveRate ?? null,
          myRating: data.myRating ?? null,
        });
      } else if (ratingResult.reason?.code !== "ERR_CANCELED") {
        console.error("영화 평가 조회 실패:", ratingResult.reason);
        setRatingError("영화 평가를 불러오지 못했습니다.");
      }
    }

    if (id) fetchReviewsAndRating();
    return () => {
      active = false;
      controller.abort();
    };
  }, [id]);

  // =========================================================
  // 6. 영화 평가 데이터
  // =========================================================

  const likeCount = ratingSummary.likeCount || 0;
  const dislikeCount = ratingSummary.dislikeCount || 0;
  const totalCount = ratingSummary.totalCount || 0;
  const myRating = ratingSummary.myRating;

  // 좋아요 비율을 5점 만점 평점으로 환산
  const movieScore =
    totalCount > 0 ? ((likeCount / totalCount) * 5).toFixed(2) : "0.00";

  // =========================================================
  // 7. 영화 평가 갱신
  // =========================================================

  async function refreshMovieRating() {
    const response = await AxiosApi.movieRating(movie.movieCode);
    const data = response.data || {};
    setRatingSummary({
      likeCount: data.likeCount || 0,
      dislikeCount: data.dislikeCount || 0,
      totalCount: data.totalCount || 0,
      positiveRate: data.positiveRate ?? null,
      myRating: data.myRating ?? null,
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

      // 저장 요청이 성공한 뒤의 조회 실패는 저장 실패로 취급하지 않는다.
      try {
        await refreshMovieRating();
      } catch (refreshError) {
        console.error("평가 저장 후 최신 집계 조회 실패:", refreshError);
        setRatingError(
          "평가는 저장됐지만 최신 집계를 불러오지 못했습니다. 새로고침해 주세요.",
        );
      }
    } catch (error) {
      console.error("영화 평가 처리 실패:", error);
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
    const identifier = person.name?.trim();

    if (!identifier || identifier === "이름 없음") return;

    // 백엔드는 TMDb ID가 아닌 인물 이름으로 검색한다.
    navigate(`/person/${type}/${encodeURIComponent(identifier)}`);
  }

  // =========================================================
  // 11. 영화 정보 로딩 중
  // =========================================================

  if (loading) {
    return React.createElement(
      "main",
      { className: "movie-detail-page" },
      React.createElement("p", null, "영화 정보를 불러오는 중입니다."),
    );
  }

  // =========================================================
  // 12. 영화 정보 조회 실패
  // =========================================================

  if (error || !movie) {
    return React.createElement(
      "main",
      { className: "movie-detail-page" },
      React.createElement(
        "button",
        { className: "detail-back-button", onClick: () => navigate("/") },
        "← 메인으로 돌아가기",
      ),
      React.createElement("p", null, error || "영화 정보를 찾을 수 없습니다."),
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

  return React.createElement(
    "main",
    { className: "movie-detail-page" },
    React.createElement(
      "button",
      { className: "detail-back-button", onClick: () => navigate("/") },
      "← 메인으로 돌아가기",
    ),
    React.createElement(
      "section",
      { className: "movie-detail-header" },
      React.createElement(
        "div",
        { className: "movie-detail-poster" },
        movie.posterUrl
          ? React.createElement("img", {
              src: resolvePosterUrl(movie.posterUrl),
              alt: movie.title,
            })
          : React.createElement(
              "div",
              { className: "poster-placeholder" },
              React.createElement("span", null, "🎬"),
              React.createElement("p", null, movie.title),
            ),
      ),
      React.createElement(
        "div",
        { className: "movie-detail-info" },
        React.createElement(
          "span",
          { className: "detail-rank" },
          "영화 상세정보",
        ),
        React.createElement("h1", null, movie.title),
        React.createElement(
          "p",
          null,
          "영문 제목: ",
          movie.titleEn || "정보 없음",
        ),
        React.createElement(
          "p",
          null,
          "개봉일: ",
          formatReleaseDate(movie.releaseDate),
        ),
        React.createElement(
          "p",
          null,
          "상영시간:",
          " ",
          movie.runningTime
            ? `${formatNumber(movie.runningTime)}분`
            : "정보 없음",
        ),
        React.createElement("p", null, "장르: ", movie.genre || "정보 없음"),
        React.createElement(
          "p",
          null,
          "관람등급: ",
          movie.watchGrade || "정보 없음",
        ),
      ),
    ),
    React.createElement(
      "section",
      { className: "movie-rating-panel" },
      React.createElement("h2", { className: "rating-label" }, "관객 평점"),
      React.createElement(
        "div",
        { className: "movie-score-display" },
        React.createElement(
          "div",
          { className: "movie-score-value" },
          React.createElement("strong", null, movieScore),
          React.createElement("span", null, "/ 5.00"),
        ),
        React.createElement(
          "p",
          null,
          "총 ",
          formatNumber(totalCount),
          "명이 평가했습니다.",
        ),
      ),
      React.createElement(
        "div",
        { className: "detail-reaction-counts" },
        React.createElement(
          "button",
          {
            type: "button",
            className: `detail-reaction-count ${
              myRating === true ? "selected-like" : ""
            }`,
            onClick: () => handleMovieRating(true),
            disabled: isRatingSubmitting,
            "aria-pressed": myRating === true,
          },
          React.createElement("span", null, "👍 좋아요"),
          React.createElement("strong", null, formatNumber(likeCount)),
        ),
        React.createElement(
          "button",
          {
            type: "button",
            className: `detail-reaction-count ${
              myRating === false ? "selected-dislike" : ""
            }`,
            onClick: () => handleMovieRating(false),
            disabled: isRatingSubmitting,
            "aria-pressed": myRating === false,
          },
          React.createElement("span", null, "👎 싫어요"),
          React.createElement("strong", null, formatNumber(dislikeCount)),
        ),
      ),
      React.createElement(
        "p",
        { className: "detail-total-count" },
        "전체 평가 수: ",
        React.createElement("strong", null, formatNumber(totalCount)),
      ),
      isRatingSubmitting &&
        React.createElement(
          "p",
          { className: "rating-status" },
          "평가를 저장하고 있습니다.",
        ),
      ratingError &&
        React.createElement("p", { className: "people-empty" }, ratingError),
    ),
    React.createElement(
      "section",
      { className: "detail-content-section" },
      React.createElement("h2", null, "감독"),
      directors.length > 0
        ? React.createElement(
            "div",
            { className: "people-list" },
            directors.map((person, index) =>
              React.createElement(
                "button",
                {
                  type: "button",
                  className: "person-chip",
                  key: `${person.name}-${index}`,
                  onClick: () => moveToPersonDetail(person, "director"),
                },
                person.name,
              ),
            ),
          )
        : React.createElement(
            "p",
            { className: "people-empty" },
            "감독 정보가 아직 연결되지 않았습니다.",
          ),
    ),
    React.createElement(
      "section",
      { className: "detail-content-section" },
      React.createElement("h2", null, "출연 배우"),
      actors.length > 0
        ? React.createElement(
            "div",
            { className: "people-list" },
            actors.map((person, index) =>
              React.createElement(
                "button",
                {
                  type: "button",
                  className: "person-chip",
                  key: `${person.name}-${index}`,
                  onClick: () => moveToPersonDetail(person, "actor"),
                },
                person.name,
                person.role &&
                  React.createElement(
                    "span",
                    { className: "person-role" },
                    " (",
                    person.role,
                    ")",
                  ),
              ),
            ),
          )
        : React.createElement(
            "p",
            { className: "people-empty" },
            "배우 정보가 아직 연결되지 않았습니다.",
          ),
    ),
    React.createElement(
      "section",
      { className: "detail-content-section" },
      React.createElement("h2", null, "영화 메모"),
      reviewError &&
        React.createElement("p", { className: "people-empty" }, reviewError),
      React.createElement(
        "div",
        { className: "review-write-section" },
        React.createElement(
          "button",
          {
            type: "button",
            className: "review-write-toggle",
            onClick: () => setIsReviewOpen((prev) => !prev),
          },
          React.createElement(
            "span",
            null,
            "✏️ ",
            movie.title,
            " 영화 메모 작성하기",
          ),
          React.createElement(
            "span",
            { className: "review-toggle-arrow" },
            isReviewOpen ? "▲" : "▼",
          ),
        ),
        isReviewOpen &&
          React.createElement(
            "div",
            { className: "review-write-content" },
            React.createElement("h3", null, "✏️ 이 영화, 어떻게 기억하시나요?"),
            React.createElement(
              "div",
              { className: "review-title-field" },
              React.createElement(
                "div",
                { className: "review-field-heading" },
                React.createElement(
                  "label",
                  { htmlFor: "reviewTitle" },
                  "메모 제목",
                ),
                React.createElement("span", null, reviewTitle.length, "/100"),
              ),
              React.createElement("input", {
                id: "reviewTitle",
                type: "text",
                value: reviewTitle,
                onChange: (e) => setReviewTitle(e.target.value),
                placeholder: "영화 메모의 제목을 입력해 주세요.",
                maxLength: 100,
              }),
            ),
            React.createElement(
              "div",
              { className: "review-content-field" },
              React.createElement(
                "div",
                { className: "review-field-heading" },
                React.createElement(
                  "label",
                  { htmlFor: "reviewContent" },
                  "영화 메모",
                ),
                React.createElement("span", null, reviewText.length, "/2000"),
              ),
              React.createElement("textarea", {
                id: "reviewContent",
                value: reviewText,
                onChange: (e) => setReviewText(e.target.value),
                placeholder: "영화를 보고 느낀 생각을 자유롭게 기록해 보세요.",
                maxLength: 2000,
              }),
            ),
            React.createElement(
              "div",
              { className: "review-form-actions" },
              React.createElement(
                "button",
                {
                  type: "button",
                  onClick: addReview,
                  disabled: isReviewSubmitting,
                },
                isReviewSubmitting ? "저장 중..." : "✏️ 기록하기",
              ),
            ),
          ),
      ),
      reviews.length > 0
        ? React.createElement(
            "div",
            { className: "detail-review-list" },
            reviews.map((review) =>
              React.createElement(
                "article",
                { className: "detail-review-card", key: review.reviewId },
                React.createElement(
                  "div",
                  null,
                  React.createElement("strong", null, review.title),
                  React.createElement(
                    "span",
                    null,
                    review.createdAt
                      ? new Date(review.createdAt).toLocaleDateString("ko-KR")
                      : "",
                  ),
                ),
                React.createElement("p", null, review.content),
              ),
            ),
          )
        : React.createElement(
            "p",
            { className: "people-empty" },
            "아직 작성된 영화 메모가 없습니다.",
          ),
    ),
  );
}

export default MovieDetail;
