import { createGlobalStyle } from "styled-components";

// 기존 className과 선택자를 그대로 유지하는 호환 스타일 모듈입니다.
const MyReviewsStyles = createGlobalStyle`
/* ==================================================
   MY REVIEWS
   마이페이지 작성 리뷰 모아보기
================================================== */

.my-reviews-page {
  width: calc(100% - 40px);
  max-width: 1320px;

  margin: 0 auto;
  padding: 30px 0 80px;
}

.my-reviews-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;

  padding: 36px;
  margin-bottom: 30px;

  background: #fffaf0;
  border: 1px solid #f0dfbd;
  border-radius: 14px;
}

.my-reviews-eyebrow {
  margin: 0 0 10px;

  color: #b78000;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1px;
}

.my-reviews-heading h1 {
  margin: 0 0 10px;

  color: #111;
  font-size: 30px;
  font-weight: 800;
}

.my-reviews-heading p {
  margin: 0;

  color: #817565;
  font-size: 14px;
}

.my-reviews-heading > a {
  color: #9c6c00;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
}

.my-reviews-heading > a:hover {
  text-decoration: underline;
}

.my-reviews-card {
  padding: 30px;

  background: #fff;
  border: 1px solid #e5dfd6;
  border-radius: 14px;
}

.my-reviews-card-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;

  margin-bottom: 24px;
  padding-bottom: 18px;

  border-bottom: 1px solid #e8e3dc;
}

.my-reviews-card-heading h2 {
  margin: 0 0 8px;

  color: #111;
  font-size: 20px;
  font-weight: 800;
}

.my-reviews-card-heading p {
  margin: 0;

  color: #817565;
  font-size: 13px;
}

.my-reviews-card-heading > span {
  padding: 7px 12px;

  background: #fffaf0;
  border: 1px solid #f0dfbd;
  border-radius: 8px;

  color: #9c6c00;
  font-size: 12px;
  font-weight: 800;
}

/* 리뷰 리스트 */

.my-reviews-list {
  display: flex;
  flex-direction: column;
}

.my-review-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;

  padding: 22px 12px;

  border-bottom: 1px solid #e8e3dc;

  color: #111;
  text-decoration: none;

  transition: background 0.15s ease;
}

.my-review-item:hover {
  background: #fffcf4;
}

.my-review-info {
  flex: 1;
  min-width: 0;
}

.my-review-movie {
  color: #9c6c00;
  font-size: 12px;
  font-weight: 800;
}

.my-review-info h3 {
  margin: 8px 0;

  color: #111;
  font-size: 18px;
  font-weight: 800;
}

.my-review-info p {
  margin: 0 0 10px;

  color: #6d665d;
  font-size: 14px;
  line-height: 1.6;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.my-review-date {
  color: #999;
  font-size: 12px;
}

.my-review-arrow {
  color: #b78000;
  font-size: 22px;
}

/* 리뷰 목록 상태 */

.my-reviews-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;

  min-height: 200px;

  color: #777;
  text-align: center;
}

.my-reviews-state a {
  color: #9c6c00;
  font-weight: 800;
  text-decoration: none;
}

.my-reviews-state.error {
  color: #e53935;
}

/* 페이지네이션 */

.my-reviews-pagination {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;

  margin-top: 28px;
}

.my-reviews-pagination button {
  min-width: 38px;
  height: 38px;

  padding: 0 12px;

  background: #fff;
  border: 1px solid #e5dfd6;
  border-radius: 8px;

  color: #555;
  font-weight: 700;

  cursor: pointer;
}

.my-reviews-pagination button.active {
  background: #ffd43b;
  border-color: #ffd43b;
  color: #111;
}

.my-reviews-pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 마이페이지 작성 리뷰 카드 링크 */

.profile-reviews-link {
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}

.profile-reviews-link:hover {
  background: #fffaf0;
}

.profile-reviews-count {
  color: #b78000;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

/* ==================================================
   ALL REVIEWS CLICKABLE ROW
================================================== */

.review-clickable-row {
  cursor: pointer;
}

.review-clickable-row:hover {
  background: #fffaf0;
}

.all-reviews-mobile-item {
  cursor: pointer;
}

.all-reviews-mobile-item:hover {
  background: #fffaf0;
}

`;

export default MyReviewsStyles;
