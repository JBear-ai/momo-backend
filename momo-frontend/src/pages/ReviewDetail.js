import React, { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";
import ProfileAvatar from "../components/ProfileAvatar";

const h = React.createElement;

function ReviewDetail() {
  const { reviewId } = useParams();
  const navigate = useNavigate();

  const [review, setReview] = useState(null);
  const [movie, setMovie] = useState(null);
  const [posterUrl, setPosterUrl] = useState("");

  const [comments, setComments] = useState([]);
  const [commentsTotal, setCommentsTotal] = useState(0);
  const [currentUser, setCurrentUser] = useState(null);

  const [likeCount, setLikeCount] = useState(0);
  const [dislikeCount, setDislikeCount] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [commentContent, setCommentContent] = useState("");
  const [commentLoading, setCommentLoading] = useState(false);

  const [editingReview, setEditingReview] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editContent, setEditContent] = useState("");

  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editCommentContent, setEditCommentContent] = useState("");

  const [myReaction, setMyReaction] = useState(null);

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
  const fetchReview = async () => {
    const response = await AxiosApi.reviewDetail(reviewId);

    setReview(response.data);
    setEditTitle(response.data.title || "");
    setEditContent(response.data.content || "");

    return response.data;
  };

  // 댓글 목록 조회
  const fetchComments = async () => {
    try {
      const response = await AxiosApi.reviewComments(reviewId, 0);

      setComments(response.data.content || []);
      setCommentsTotal(
        response.data.page?.totalElements ?? response.data.totalElements ?? response.data.content?.length ?? 0,
      );
    } catch (err) {
      console.error("댓글 목록 조회 실패:", err);
    }
  };

  // 좋아요 / 싫어요 개수 조회
  const fetchReactionCounts = async () => {
    try {
      const response = await AxiosApi.reviewReactionCounts(reviewId);

      setLikeCount(response.data.likeCount || 0);
      setDislikeCount(response.data.dislikeCount || 0);
    } catch (err) {
      console.error("리뷰 반응 개수 조회 실패:", err);
    }
  };

  // 초기 데이터 조회
  useEffect(() => {
    let active = true;

    const loadData = async () => {
      try {
        setLoading(true);
        setError("");

        // 리뷰 상세는 페이지 진입에 필수
        const reviewResponse = await AxiosApi.reviewDetail(reviewId);

        if (!active) return;

        const reviewData = reviewResponse.data;

        setReview(reviewData);
        setEditTitle(reviewData.title || "");
        setEditContent(reviewData.content || "");

        // 나머지 데이터는 각각 독립적으로 조회
        const results = await Promise.allSettled([
          AxiosApi.reviewComments(reviewId, 0),
          AxiosApi.memberMe(),
          AxiosApi.reviewReactionCounts(reviewId),
          reviewData.movieCode
            ? AxiosApi.movieDetail(reviewData.movieCode)
            : Promise.resolve(null),
          reviewData.movieCode
            ? AxiosApi.moviePoster(reviewData.movieCode)
            : Promise.resolve(null),
        ]);

        if (!active) return;

        // 댓글
        if (results[0].status === "fulfilled") {
          const data = results[0].value.data;

          setComments(data.content || []);
          setCommentsTotal(data.page?.totalElements ?? data.totalElements ?? data.content?.length ?? 0);
        } else {
          console.error("댓글 조회 실패:", results[0].reason);
        }

        // 로그인 사용자
        if (results[1].status === "fulfilled") {
          const userData = results[1].value.data;
          setCurrentUser(userData);

          // 로그인 회원의 현재 리뷰 반응을 조회한다.
          if (userData) {
            try {
              const reactionResponse = await AxiosApi.reviewMyReactionGet(reviewId);
              if (!active) return;
              setMyReaction(reactionResponse.data ?? null);
            } catch (reactionError) {
              if (reactionError.response?.status !== 401) {
                console.error("내 리뷰 반응 조회 실패:", reactionError);
              }
              setMyReaction(null);
            }
          } else {
            setMyReaction(null);
          }
        } else {
          setCurrentUser(null);
          setMyReaction(null);
        }

        // 좋아요 / 싫어요 개수
        if (results[2].status === "fulfilled") {
          const data = results[2].value.data;

          setLikeCount(data.likeCount || 0);
          setDislikeCount(data.dislikeCount || 0);
        } else {
          console.error("반응 개수 조회 실패:", results[2].reason);
        }

        // 영화 정보
        if (results[3].status === "fulfilled" && results[3].value) {
          setMovie(results[3].value.data);
        } else if (results[3].status === "rejected") {
          console.error("영화 정보 조회 실패:", results[3].reason);
        }

        // 영화 포스터
        if (results[4].status === "fulfilled" && results[4].value) {
          setPosterUrl(results[4].value.data.posterUrl || "");
        } else if (results[4].status === "rejected") {
          console.error("영화 포스터 조회 실패:", results[4].reason);
        }
      } catch (err) {
        if (!active) return;

        console.error("리뷰 상세 조회 실패:", err);

        if (err.response?.status === 404) {
          setError("존재하지 않는 리뷰입니다.");
        } else if (err.response?.status === 401) {
          setError("리뷰를 조회할 권한이 없습니다.");
        } else {
          setError("리뷰 정보를 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadData();

    return () => {
      active = false;
    };
  }, [reviewId]);

  // 리뷰 작성자 확인
  const isReviewAuthor =
    currentUser &&
    review &&
    Number(currentUser.userId) === Number(review.userId);

  // 댓글 작성자 확인
  const isCommentAuthor = (comment) => {
    return currentUser && Number(currentUser.userId) === Number(comment.userId);
  };

  // 리뷰 수정
  const handleReviewUpdate = async () => {
    if (!editTitle.trim() || !editContent.trim()) {
      alert("리뷰 제목과 내용을 입력해주세요.");
      return;
    }

    if (editTitle.trim().length > 100) {
      alert("리뷰 제목은 100자 이하로 입력해주세요.");
      return;
    }

    if (editContent.trim().length > 2000) {
      alert("리뷰 내용은 2000자 이하로 입력해주세요.");
      return;
    }

    try {
      await AxiosApi.reviewUpdate(
        reviewId,
        editTitle.trim(),
        editContent.trim(),
      );

      alert("리뷰가 수정되었습니다.");

      setEditingReview(false);

      await fetchReview();
    } catch (err) {
      console.error("리뷰 수정 실패:", err);

      if (err.response?.status === 403) {
        alert("본인이 작성한 리뷰만 수정할 수 있습니다.");
      } else {
        alert(err.response?.data?.message || "리뷰 수정에 실패했습니다.");
      }
    }
  };

  // 리뷰 수정 취소
  const handleReviewEditCancel = () => {
    setEditingReview(false);
    setEditTitle(review.title || "");
    setEditContent(review.content || "");
  };

  // 리뷰 삭제
  const handleReviewDelete = async () => {
    const confirmed = window.confirm(
      "정말 이 리뷰를 삭제하시겠습니까?\n댓글도 함께 삭제됩니다.",
    );

    if (!confirmed) return;

    try {
      await AxiosApi.reviewDelete(reviewId);

      alert("리뷰가 삭제되었습니다.");

      navigate("/reviews", { replace: true });
    } catch (err) {
      console.error("리뷰 삭제 실패:", err);

      if (err.response?.status === 403) {
        alert("본인이 작성한 리뷰만 삭제할 수 있습니다.");
      } else {
        alert(err.response?.data?.message || "리뷰 삭제에 실패했습니다.");
      }
    }
  };

  // 댓글 작성
  const handleCommentCreate = async (e) => {
    e.preventDefault();

    if (!currentUser) {
      alert("로그인 후 댓글을 작성해주세요.");
      navigate("/login");
      return;
    }

    if (!commentContent.trim()) {
      alert("댓글 내용을 입력해주세요.");
      return;
    }

    if (commentContent.trim().length > 500) {
      alert("댓글은 500자 이하로 입력해주세요.");
      return;
    }

    try {
      setCommentLoading(true);

      await AxiosApi.commentCreate(reviewId, commentContent.trim());

      setCommentContent("");

      await fetchComments();
    } catch (err) {
      console.error("댓글 작성 실패:", err);

      if (err.response?.status === 401) {
        alert("로그인이 필요합니다.");
      } else {
        alert(err.response?.data?.message || "댓글 작성에 실패했습니다.");
      }
    } finally {
      setCommentLoading(false);
    }
  };

  // 댓글 수정 시작
  const handleCommentEditStart = (comment) => {
    setEditingCommentId(comment.commentId);
    setEditCommentContent(comment.content || "");
  };

  // 댓글 수정 취소
  const handleCommentEditCancel = () => {
    setEditingCommentId(null);
    setEditCommentContent("");
  };

  // 댓글 수정 저장
  const handleCommentUpdate = async (commentId) => {
    if (!editCommentContent.trim()) {
      alert("댓글 내용을 입력해주세요.");
      return;
    }

    if (editCommentContent.trim().length > 500) {
      alert("댓글은 500자 이하로 입력해주세요.");
      return;
    }

    try {
      await AxiosApi.commentUpdate(commentId, editCommentContent.trim());

      setEditingCommentId(null);
      setEditCommentContent("");

      await fetchComments();
    } catch (err) {
      console.error("댓글 수정 실패:", err);

      if (err.response?.status === 403) {
        alert("본인이 작성한 댓글만 수정할 수 있습니다.");
      } else {
        alert(err.response?.data?.message || "댓글 수정에 실패했습니다.");
      }
    }
  };

  // 댓글 반응: 같은 버튼 재클릭은 취소, 반대 버튼은 변경
  const handleCommentReaction = async (commentId, liked, currentReaction) => {
    if (!currentUser) {
      alert("로그인 후 이용해주세요.");
      navigate("/login");
      return;
    }

    try {
      if (currentReaction === liked) {
        await AxiosApi.commentReactionDelete(commentId);
      } else {
        await AxiosApi.commentReaction(commentId, liked);
      }
      await fetchComments();
    } catch (err) {
      console.error("댓글 반응 처리 실패:", err);
      alert(err.response?.data?.message || "댓글 반응 처리에 실패했습니다.");
    }
  };

  // 댓글 삭제
  const handleCommentDelete = async (commentId) => {
    const confirmed = window.confirm("댓글을 삭제하시겠습니까?");

    if (!confirmed) return;

    try {
      await AxiosApi.commentDelete(commentId);

      await fetchComments();
    } catch (err) {
      console.error("댓글 삭제 실패:", err);

      if (err.response?.status === 403) {
        alert("본인이 작성한 댓글만 삭제할 수 있습니다.");
      } else {
        alert(err.response?.data?.message || "댓글 삭제에 실패했습니다.");
      }
    }
  };

  // 좋아요 / 싫어요 저장 및 변경
  const handleReaction = async (liked) => {
    if (!currentUser) {
      alert("로그인 후 이용해주세요.");
      navigate("/login");
      return;
    }

    try {
      if (myReaction === liked) {
        await AxiosApi.reviewReactionDelete(reviewId);
        setMyReaction(null);
      } else {
        await AxiosApi.reviewReaction(reviewId, liked);
        setMyReaction(liked);
      }

      await fetchReactionCounts();
    } catch (err) {
      console.error("리뷰 반응 처리 실패:", err);

      if (err.response?.status === 401) {
        alert("로그인이 필요합니다.");
      } else {
        alert(err.response?.data?.message || "반응 처리에 실패했습니다.");
      }
    }
  };

  // 로딩 화면
  if (loading) {
    return h(
      "div",
      { className: "review-detail-page" },
      h(
        "div",
        { className: "review-detail-state" },
        "리뷰를 불러오는 중입니다.",
      ),
    );
  }

  // 오류 화면
  if (error || !review) {
    return h(
      "div",
      { className: "review-detail-page" },
      h(
        Link,
        {
          to: "/reviews",
          className: "review-detail-back",
        },
        "← 목록으로",
      ),
      h(
        "div",
        {
          className: "review-detail-state review-detail-error",
        },
        error || "리뷰 정보를 찾을 수 없습니다.",
      ),
    );
  }

  // 리뷰 상단
  const reviewHeader = h(
    "div",
    { className: "review-detail-top" },

    h(
      "div",
      { className: "review-detail-main" },

      h("p", { className: "review-detail-eyebrow" }, "MOMO COMMUNITY REVIEW"),

      editingReview
        ? h("input", {
            className: "review-edit-title",
            value: editTitle,
            maxLength: 100,
            onChange: (e) => setEditTitle(e.target.value),
            placeholder: "리뷰 제목을 입력해주세요.",
          })
        : h("h1", { className: "review-detail-title" }, review.title),

      h(
        "div",
        { className: "review-detail-meta" },

        h(
          "span",
          null,
          "작성자 ",
          h("strong", null, review.nickname || "익명"),
        ),

        h("span", null, "작성일 ", formatDate(review.createdAt)),

        review.updatedAt &&
          new Date(review.updatedAt).getTime() >
            new Date(review.createdAt).getTime() &&
          h("span", null, "수정일 ", formatDate(review.updatedAt)),
      ),
    ),

    isReviewAuthor &&
      !editingReview &&
      h(
        "div",
        { className: "review-detail-actions" },

        h(
          "button",
          {
            type: "button",
            className: "review-action-button",
            onClick: () => setEditingReview(true),
          },
          "수정",
        ),

        h(
          "button",
          {
            type: "button",
            className: "review-action-button delete",
            onClick: handleReviewDelete,
          },
          "삭제",
        ),
      ),
  );

  // 리뷰 수정 영역
  const reviewEditArea = editingReview
    ? h(
        "div",
        { className: "review-edit-area" },

        h("textarea", {
          value: editContent,
          maxLength: 2000,
          onChange: (e) => setEditContent(e.target.value),
          placeholder: "리뷰 내용을 입력해주세요.",
          rows: 8,
        }),

        h(
          "div",
          { className: "review-edit-buttons" },

          h(
            "button",
            {
              type: "button",
              onClick: handleReviewUpdate,
            },
            "수정 완료",
          ),

          h(
            "button",
            {
              type: "button",
              className: "cancel",
              onClick: handleReviewEditCancel,
            },
            "취소",
          ),
        ),
      )
    : null;

  // 영화 정보 영역
  const movieSection = h(
    "section",
    { className: "review-detail-movie" },

    h(
      "div",
      { className: "review-detail-movie-poster" },

      posterUrl
        ? h("img", {
            src: posterUrl,
            alt: movie?.title || "영화 포스터",
          })
        : h("div", { className: "review-detail-poster-placeholder" }, "MOMO"),
    ),

    h(
      "div",
      { className: "review-detail-movie-info" },

      h("span", { className: "review-detail-movie-label" }, "REVIEW MOVIE"),

      h("h2", null, movie?.title || review.movieCode || "영화 정보 없음"),

      movie?.titleEn && h("p", null, movie.titleEn),

      h(
        "div",
        { className: "review-detail-movie-meta" },

        movie?.genre && h("span", null, movie.genre),

        movie?.releaseDate && h("span", null, movie.releaseDate + " 개봉"),

        movie?.runningTime && h("span", null, movie.runningTime + "분"),
      ),

      h(
        Link,
        {
          to: `/movie/${review.movieCode}`,
          className: "review-detail-movie-link",
        },
        "영화 상세보기 →",
      ),
    ),
  );

  // 좋아요 / 싫어요 영역
  const reactionSection = h(
    "div",
    { className: "review-detail-reactions" },

    h(
      "button",
      {
        type: "button",
        className:
          "review-like-button" + (myReaction === true ? " active" : ""),
        onClick: () => handleReaction(true),
      },
      "좋아요 ",
      likeCount,
    ),

    h(
      "button",
      {
        type: "button",
        className:
          "review-dislike-button" + (myReaction === false ? " active" : ""),
        onClick: () => handleReaction(false),
      },
      "싫어요 ",
      dislikeCount,
    ),


  );

  // 리뷰 본문 영역
  const reviewContentSection = h(
    "section",
    { className: "review-detail-content-section" },

    h("p", { className: "review-detail-eyebrow" }, "REVIEW CONTENT"),

    !editingReview &&
      h("div", { className: "review-detail-content" }, review.content),
  );

  // 댓글 작성 영역
  const commentForm = h(
    "form",
    {
      className: "review-comment-form",
      onSubmit: handleCommentCreate,
    },

    h(
      "div",
      { className: "review-comment-avatar" },
      h(ProfileAvatar, {
        userId: currentUser?.userId,
        hasImage: Boolean(currentUser?.profileImage),
        alt: "내 프로필 이미지",
      }),
    ),

    h("input", {
      type: "text",
      value: commentContent,
      onChange: (e) => setCommentContent(e.target.value),
      placeholder: currentUser
        ? "댓글을 입력하세요..."
        : "로그인 후 댓글을 작성할 수 있습니다.",
      maxLength: 500,
      disabled: commentLoading,
    }),

    h(
      "button",
      {
        type: "submit",
        disabled: commentLoading,
      },
      commentLoading ? "등록 중..." : "댓글 등록",
    ),
  );

  // 댓글 목록
  const commentList =
    comments.length === 0
      ? h(
          "div",
          { className: "review-comment-empty" },
          "아직 댓글이 없습니다.",
          h("br"),
          "첫 댓글을 작성해보세요!",
        )
      : comments.map((comment) =>
          h(
            "article",
            {
              className: "review-comment-item",
              key: comment.commentId,
            },

            h(
              "div",
              { className: "review-comment-avatar" },
              h(ProfileAvatar, {
                userId: comment.userId,
                hasImage: Boolean(comment.profileImageAvailable),
                alt: `${comment.nickname || "회원"} 프로필 이미지`,
              }),
            ),

            h(
              "div",
              { className: "review-comment-body" },

              h(
                "div",
                { className: "review-comment-meta" },

                h("strong", null, comment.nickname || "익명"),

                h("span", null, formatDate(comment.createdAt)),

                isCommentAuthor(comment) &&
                  editingCommentId !== comment.commentId &&
                  h(
                    "div",
                    { className: "review-comment-actions" },

                    h(
                      "button",
                      {
                        type: "button",
                        onClick: () => handleCommentEditStart(comment),
                      },
                      "수정",
                    ),

                    h(
                      "button",
                      {
                        type: "button",
                        className: "delete",
                        onClick: () => handleCommentDelete(comment.commentId),
                      },
                      "삭제",
                    ),
                  ),
              ),

              editingCommentId === comment.commentId
                ? h(
                    "div",
                    { className: "review-comment-edit" },

                    h("textarea", {
                      value: editCommentContent,
                      maxLength: 500,
                      onChange: (e) => setEditCommentContent(e.target.value),
                    }),

                    h(
                      "div",
                      null,

                      h(
                        "button",
                        {
                          type: "button",
                          onClick: () => handleCommentUpdate(comment.commentId),
                        },
                        "저장",
                      ),

                      h(
                        "button",
                        {
                          type: "button",
                          className: "cancel",
                          onClick: handleCommentEditCancel,
                        },
                        "취소",
                      ),
                    ),
                  )
                : h(
                    "p",
                    { className: "review-comment-content" },
                    comment.content,
                  ),

              h(
                "div",
                { className: "review-comment-reactions" },

                h(
                  "button",
                  {
                    type: "button",
                    className:
                      "comment-like-button" +
                      (comment.myReaction === true ? " active" : ""),
                    onClick: () =>
                      handleCommentReaction(comment.commentId, true, comment.myReaction),
                  },
                  `좋아요 ${comment.likeCount ?? 0}`,
                ),

                h(
                  "button",
                  {
                    type: "button",
                    className:
                      "comment-dislike-button" +
                      (comment.myReaction === false ? " active" : ""),
                    onClick: () =>
                      handleCommentReaction(comment.commentId, false, comment.myReaction),
                  },
                  `싫어요 ${comment.dislikeCount ?? 0}`,
                ),


              ),
            ),
          ),
        );

  // 최종 화면
  return h(
    "div",
    { className: "review-detail-page" },

    h(
      Link,
      {
        to: "/reviews",
        className: "review-detail-back",
      },
      "← 목록으로",
    ),

    h(
      "article",
      { className: "review-detail-card" },

      reviewHeader,
      reviewEditArea,
      movieSection,
      reactionSection,
      reviewContentSection,
    ),

    h(
      "section",
      { className: "review-comment-card" },

      h(
        "div",
        { className: "review-comment-heading" },
        h("h2", null, `댓글 (${commentsTotal})`),
      ),

      commentForm,

      h("div", { className: "review-comment-list" }, commentList),
    ),
  );
}

export default ReviewDetail;
