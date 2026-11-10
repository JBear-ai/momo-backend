import { createGlobalStyle } from "styled-components";

// 기존 className과 선택자를 그대로 유지하는 호환 스타일 모듈입니다.
const ReviewDetailCommentsStyles = createGlobalStyle`
/* ==================================================
   MOMO COMMUNITY REVIEW DETAIL
   리뷰 상세 페이지
================================================== */

.review-detail-page {
  width: calc(100% - 40px);
  max-width: 1320px;
  margin: 0 auto;
  padding: 20px 0 60px;
}

.review-detail-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-bottom: 12px;

  background: #fff;
  border: 1px solid #d8d8d8;
  border-radius: 9px;

  color: #111;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;

  cursor: pointer;
}

.review-detail-back:hover {
  background: #fffaf0;
  border-color: #d5a22c;
}

.review-detail-card {
  padding: 28px;

  background: #fff;
  border: 1px solid #e5dfd6;
  border-radius: 16px;

  box-shadow: 0 8px 24px rgba(45, 38, 26, 0.06);
}

.review-detail-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;

  padding-bottom: 20px;
  border-bottom: 1px solid #e8e3dc;
}

.review-detail-main {
  flex: 1;
  min-width: 0;
}

.review-detail-eyebrow {
  margin: 0 0 10px;

  color: #ae7600;
  font-size: 14px;
  font-weight: 800;
}

.review-detail-title {
  margin: 0 0 12px;

  color: #111;
  font-size: 32px;
  font-weight: 800;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.review-detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;

  color: #555;
  font-size: 14px;
}

.review-detail-meta span {
  padding-right: 12px;
  border-right: 1px solid #e5e5e5;
}

.review-detail-meta span:last-child {
  border-right: none;
}

.review-detail-meta strong {
  color: #111;
}

.review-detail-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.review-action-button {
  padding: 10px 18px;

  background: #fff;
  border: 1px solid #d5d5d5;
  border-radius: 10px;

  color: #111;
  font-size: 14px;
  font-weight: 700;

  cursor: pointer;
}

.review-action-button:hover {
  background: #fff9ec;
  border-color: #c18a00;
}

.review-action-button.delete {
  color: #e53935;
  border-color: #ffb4b4;
}

.review-action-button.delete:hover {
  background: #fff0f0;
}

/* 리뷰 수정 */

.review-edit-title {
  width: 100%;
  max-width: 700px;

  padding: 12px;
  margin-bottom: 12px;

  border: 1px solid #e5d7b9;
  border-radius: 8px;

  font-size: 24px;
  font-weight: 700;

  box-sizing: border-box;
}

.review-edit-area {
  margin-top: 20px;
}

.review-edit-area textarea {
  width: 100%;
  min-height: 200px;

  padding: 18px;

  border: 1px solid #e5d7b9;
  border-radius: 10px;

  font-size: 15px;
  line-height: 1.8;

  resize: vertical;
  box-sizing: border-box;
}

.review-edit-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;

  margin-top: 12px;
}

.review-edit-buttons button {
  padding: 10px 18px;

  background: #b78000;
  border: 1px solid #b78000;
  border-radius: 8px;

  color: #fff;
  font-weight: 700;

  cursor: pointer;
}

.review-edit-buttons .cancel {
  background: #fff;
  border-color: #ddd;
  color: #555;
}

/* 영화 정보 */

.review-detail-movie {
  display: flex;
  align-items: center;
  gap: 24px;

  padding: 20px;
  margin: 20px 0;

  background: #fffaf0;
  border: 1px solid #f0dfbd;
  border-radius: 12px;
}

.review-detail-movie-poster {
  width: 100px;
  min-width: 100px;
  height: 145px;

  overflow: hidden;

  background: #eee;
  border-radius: 8px;
}

.review-detail-movie-poster img {
  width: 100%;
  height: 100%;

  object-fit: cover;
}

.review-detail-poster-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;

  width: 100%;
  height: 100%;

  color: #b78000;
  font-weight: 800;
}

.review-detail-movie-info {
  flex: 1;
  min-width: 0;
}

.review-detail-movie-label {
  display: block;
  margin-bottom: 8px;

  color: #6b6254;
  font-size: 12px;
  font-weight: 800;
}

.review-detail-movie-info h2 {
  margin: 0 0 6px;

  color: #111;
  font-size: 22px;
  font-weight: 800;
}

.review-detail-movie-info > p {
  margin: 0 0 10px;
  color: #777;
  font-size: 14px;
}

.review-detail-movie-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  margin-bottom: 12px;

  color: #666;
  font-size: 13px;
}

.review-detail-movie-meta span {
  padding-right: 10px;
  border-right: 1px solid #ddd;
}

.review-detail-movie-meta span:last-child {
  border-right: none;
}

.review-detail-movie-overview {
  margin: 10px 0;

  color: #666;
  font-size: 13px;
  line-height: 1.6;
}

.review-detail-movie-link {
  display: inline-block;
  margin-top: 8px;

  color: #9c6c00;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
}

.review-detail-movie-link:hover {
  text-decoration: underline;
}

/* 리뷰 내용 */

.review-detail-content-section {
  margin-top: 24px;
}

.review-detail-content {
  min-height: 120px;

  padding: 24px;

  background: #fffaf0;
  border: 1px solid #f0dfbd;
  border-radius: 12px;

  color: #111;
  font-size: 16px;
  line-height: 1.9;

  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* 좋아요 / 싫어요 */

.review-detail-reactions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;

  margin-top: 20px;
}

.review-like-button,
.review-dislike-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 150px;
  padding: 12px 20px;
  border: 1px solid transparent;
  border-radius: 12px;
  font: inherit;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.review-like-button {
  background: var(--momo-like-bg);
  border-color: var(--momo-like-border);
  color: var(--momo-like-text);
}

.review-like-button::before {
  content: "👍";
}

.review-like-button:hover,
.review-like-button.active {
  background: var(--momo-like-bg-hover);
}

.review-dislike-button {
  background: var(--momo-dislike-bg);
  border-color: var(--momo-dislike-border);
  color: var(--momo-dislike-text);
}

.review-dislike-button::before {
  content: "👎";
}

.review-dislike-button:hover,
.review-dislike-button.active {
  background: var(--momo-dislike-bg-hover);
}

.review-like-button:hover,
.review-dislike-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(45, 38, 26, 0.08);
}

.review-like-button:focus-visible,
.review-dislike-button:focus-visible {
  outline: 3px solid rgba(89, 68, 0, 0.2);
  outline-offset: 2px;
}

.review-reaction-cancel {
  padding: 10px 14px;

  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;

  color: #666;
  font-size: 13px;

  cursor: pointer;
}

/* ==================================================
   REVIEW COMMENTS
   댓글 영역
================================================== */

.review-comment-card {
  margin-top: 24px;
  padding: 28px;

  background: #fff;
  border: 1px solid #e5dfd6;
  border-radius: 16px;

  box-shadow: 0 8px 24px rgba(45, 38, 26, 0.04);
}

.review-comment-heading {
  margin-bottom: 20px;
}

.review-comment-heading h2 {
  margin: 0;

  color: #111;
  font-size: 24px;
  font-weight: 800;
}

/* 댓글 입력 */

.review-comment-form {
  display: flex;
  align-items: center;
  gap: 12px;

  margin-bottom: 20px;
}

.review-comment-avatar {
  display: flex;
  align-items: center;
  justify-content: center;

  width: 44px;
  height: 44px;
  min-width: 44px;

  background: #e8e8e8;
  border-radius: 50%;

  color: #777;
  font-size: 18px;
  font-weight: 800;
}

.review-comment-form input {
  flex: 1;
  min-width: 0;

  padding: 13px 16px;

  border: 1px solid #d5d5d5;
  border-radius: 10px;

  outline: none;
  font-size: 14px;
}

.review-comment-form input:focus {
  border-color: #c18a00;
}

.review-comment-form button {
  padding: 13px 18px;

  background: #b78000;
  border: 1px solid #b78000;
  border-radius: 10px;

  color: #fff;
  font-weight: 800;

  cursor: pointer;
}

.review-comment-form button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 댓글 목록 */

.review-comment-list {
  display: flex;
  flex-direction: column;
}

.review-comment-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;

  padding: 18px 0;

  border-bottom: 1px solid #e8e3dc;
}

.review-comment-item:last-child {
  border-bottom: none;
}

.review-comment-body {
  flex: 1;
  min-width: 0;
}

.review-comment-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;

  margin-bottom: 8px;
}

.review-comment-meta strong {
  color: #111;
  font-size: 15px;
}

.review-comment-meta > span {
  color: #777;
  font-size: 12px;
}

.review-comment-actions {
  display: flex;
  gap: 8px;

  margin-left: auto;
}

.review-comment-actions button {
  padding: 6px 10px;

  background: #fff;
  border: 1px solid #ddd;
  border-radius: 7px;

  font-size: 12px;
  font-weight: 700;

  cursor: pointer;
}

.review-comment-actions button:hover {
  background: #fffaf0;
}

.review-comment-actions button.delete {
  color: #e53935;
  border-color: #ffb4b4;
}

.review-comment-content {
  margin: 0;

  color: #222;
  font-size: 15px;
  line-height: 1.7;

  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* 댓글 수정 */

.review-comment-edit textarea {
  width: 100%;
  min-height: 90px;

  padding: 12px;
  margin-bottom: 10px;

  border: 1px solid #e5d7b9;
  border-radius: 8px;

  font-size: 14px;
  resize: vertical;

  box-sizing: border-box;
}

.review-comment-edit > div {
  display: flex;
  gap: 8px;
}

.review-comment-edit button {
  padding: 9px 16px;

  background: #b78000;
  border: 1px solid #b78000;
  border-radius: 8px;

  color: #fff;
  font-weight: 700;

  cursor: pointer;
}

.review-comment-edit .cancel {
  background: #fff;
  color: #555;
  border-color: #ddd;
}

.review-comment-empty {
  padding: 40px 20px;

  color: #888;
  text-align: center;
  font-size: 14px;
}


/* 공용 프로필 이미지 */
.review-comment-avatar img {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

/* 댓글 좋아요 / 싫어요 */
.review-comment-reactions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.comment-like-button,
.comment-dislike-button,
.comment-reaction-cancel {
  padding: 6px 10px;
  background: #fff;
  border: 1px solid #e2ddd5;
  border-radius: 7px;
  color: #666;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.comment-like-button:hover,
.comment-like-button.active {
  background: #fff6e5;
  border-color: #d6a33b;
  color: #8a5b00;
}

.comment-dislike-button:hover,
.comment-dislike-button.active {
  background: #f4f4f4;
  border-color: #999;
  color: #333;
}

.comment-reaction-cancel {
  color: #888;
}

.comment-reaction-cancel:hover {
  background: #f7f7f7;
  border-color: #bbb;
}

`;

export default ReviewDetailCommentsStyles;
