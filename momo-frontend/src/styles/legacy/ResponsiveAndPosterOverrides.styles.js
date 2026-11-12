import { createGlobalStyle } from "styled-components";

// 기존 className과 선택자를 그대로 유지하는 호환 스타일 모듈입니다.
const ResponsiveAndPosterOverridesStyles = createGlobalStyle`
/* ==================================================
   RESPONSIVE
================================================== */

@media (max-width: 768px) {
  .review-detail-page {
    width: calc(100% - 24px);
    padding: 16px 0 40px;
  }

  .review-detail-card,
  .review-comment-card {
    padding: 20px;
  }

  .review-detail-top {
    flex-direction: column;
    gap: 18px;
  }

  .review-detail-title {
    font-size: 25px;
  }

  .review-detail-actions {
    width: 100%;
  }

  .review-action-button {
    flex: 1;
  }

  .review-detail-movie {
    align-items: flex-start;
    gap: 14px;
    padding: 14px;
  }

  .review-detail-movie-poster {
    width: 76px;
    min-width: 76px;
    height: 110px;
  }

  .review-detail-movie-info h2 {
    font-size: 18px;
  }

  .review-detail-movie-overview {
    display: none;
  }

  .review-detail-content {
    padding: 18px;
    font-size: 14px;
  }

  .review-detail-reactions {
    gap: 8px;
  }

  .review-like-button,
  .review-dislike-button {
    flex: 1;
    min-width: 0;
    padding: 11px 12px;
    font-size: 13px;
  }

  .review-comment-form {
    flex-wrap: wrap;
  }

  .review-comment-form input {
    flex: 1;
    width: auto;
  }

  .review-comment-form button {
    margin-left: auto;
  }

  .review-comment-meta {
    gap: 7px;
  }

  .review-comment-actions {
    width: 100%;
    margin-left: 0;
  }

  .my-reviews-page {
    width: calc(100% - 24px);
    padding: 20px 0 50px;
  }

  .my-reviews-heading {
    flex-direction: column;
    align-items: flex-start;
    padding: 24px;
  }

  .my-reviews-heading h1 {
    font-size: 25px;
  }

  .my-reviews-card {
    padding: 20px;
  }

  .my-review-item {
    padding: 18px 6px;
  }

  .my-review-info p {
    white-space: normal;
  }
}

/* 마이페이지 영화 활동 가로 배치 */
.profile-card:has(> .profile-card-heading > div > h2) {
  /* 다른 profile-card에는 영향 주지 않도록 아래 선택자로 한정 */
}

.profile-card:has(.profile-reviews-link) {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 12px;
}

/* 제목과 설명은 카드 전체 너비 */
.profile-card:has(.profile-reviews-link) > .profile-card-heading {
  width: 100%;
}

/* 활동 카드 3개 */
.profile-card:has(.profile-reviews-link) > .profile-activity-item {
  flex: 1 1 0;
  min-width: 0;
  box-sizing: border-box;
  margin: 0;
}



/* 내가 작성한 리뷰 - 영화 포스터 */

.my-review-item {
  display: flex;
  align-items: center;
  gap: 20px;
}

.my-review-poster {
  flex-shrink: 0;
  width: 90px;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border-radius: 8px;
  background: #f0f0f0;
}

.my-review-poster img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.my-review-poster-placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 12px;
  font-weight: 700;
}

.my-review-info {
  flex: 1;
  min-width: 0;
}

.my-review-movie {
  display: block;
  margin-bottom: 8px;
  color: #777;
  font-size: 13px;
  font-weight: 600;
}

.my-review-info h3 {
  margin: 0 0 8px;
}

.my-review-info p {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  margin: 0 0 10px;
}

@media (max-width: 600px) {
  .my-review-item {
    gap: 12px;
  }

  .my-review-poster {
    width: 70px;
  }
}


/* 내가 작성한 리뷰 통계 */

.my-review-stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
  color: #777;
  font-size: 13px;
}

.my-review-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.my-review-stats strong {
  color: #333;
  font-weight: 700;
}

@media (max-width: 600px) {
  .my-review-stats {
    gap: 9px;
    font-size: 12px;
  }
}


/* 전체 리뷰 - 영화 포스터 */

.all-review-movie-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.all-review-poster {
  flex-shrink: 0;
  width: 48px;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border-radius: 5px;
  background: #f0f0f0;
}

.all-review-poster img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.all-review-poster-placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 9px;
  font-weight: 700;
}

.review-movie-title {
  display: block;
}

.all-review-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #777;
  font-size: 12px;
  white-space: nowrap;
}

.review-stats-cell {
  min-width: 100px;
}

/* 전체 리뷰 - 모바일 */

.all-review-mobile-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.all-review-mobile-info {
  flex: 1;
  min-width: 0;
}

.all-review-mobile-info .mobile-review-movie {
  margin: 0 0 7px;
}

.all-review-mobile-info .mobile-review-meta {
  margin-top: 8px;
}

.all-review-mobile-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
  color: #777;
  font-size: 12px;
}

.all-review-mobile-stats strong {
  color: #333;
}

@media (max-width: 600px) {
  .all-review-poster {
    width: 62px;
  }

  .all-review-mobile-stats {
    gap: 10px;
  }
}

`;

export default ResponsiveAndPosterOverridesStyles;
