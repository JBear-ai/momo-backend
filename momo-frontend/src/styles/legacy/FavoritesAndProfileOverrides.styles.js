import { createGlobalStyle } from "styled-components";

// 기존 className과 선택자를 그대로 유지하는 호환 스타일 모듈입니다.
const FavoritesAndProfileOverridesStyles = createGlobalStyle`
/* ========================================
   관심 영화 슬레이트 버튼
======================================== */

.favorite-star {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 5;

  display: flex;
  align-items: center;
  justify-content: center;

  box-sizing: border-box;
  width: 38px;
  height: 38px;
  padding: 0;

  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 50%;

  background: rgba(30, 30, 30, 0.38);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);

  color: rgba(255, 255, 255, 0.9);

  font-family: "Apple Color Emoji", "Segoe UI Emoji", sans-serif;
  font-size: 19px;
  font-weight: normal;
  line-height: 1;
  padding-bottom: 2.3px;


  text-align: center;
  cursor: pointer;

  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.favorite-star:hover {
  background: rgba(30, 30, 30, 0.7);
  transform: scale(1.08);
}

.favorite-star.is-active {
  background: rgba(255, 212, 59, 0.25);
  border-color: rgba(255, 212, 59, 0.7);
  color: #ffd43b;
}

/* ========================================
   홈 관심 영화 전체 보기
======================================== */

.home-favorites-more {
  padding: 8px 12px;
  border: 1px solid #e2dcce;
  border-radius: 8px;
  background: #ffffff;
  color: #4a453c;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.home-favorites-more:hover {
  border-color: #ffd43b;
  background: #fff8dc;
}

.home-favorites-empty {
  padding: 32px 20px;
  border: 1px dashed #e2dcce;
  border-radius: 12px;
  background: #ffffff;
  color: #736e63;
  font-size: 13px;
  text-align: center;
}

/* ========================================
   마이페이지 관심 영화 링크
======================================== */

.profile-favorites-link {
  color: inherit;
  text-decoration: none;
  cursor: pointer;
}

.profile-favorites-link:hover {
  border-color: #ffd43b;
  background: #fffdf4;
}

.profile-favorites-count {
  flex-shrink: 0;
  color: #a37e1c;
  font-size: 12px;
  font-weight: 800;
}

/* ========================================
   관심 영화 목록 페이지
======================================== */

.favorite-page {
  min-height: 100vh;
  padding: 56px 24px 80px;
  background: #faf9f6;
}

.favorite-page-container {
  max-width: 1240px;
  margin: 0 auto;
}

.favorite-page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 32px;
}

.favorite-page-heading > div {
  min-width: 0;
}

.favorite-page-heading .eyebrow {
  display: block;
  margin-bottom: 8px;
  color: #a37e1c;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.15em;
}

.favorite-page-heading h1 {
  margin: 0 0 8px;
  color: #1f1d1a;
  font-size: clamp(26px, 4vw, 34px);
  font-weight: 800;
}

.favorite-page-heading p,
.favorite-list-toolbar p {
  margin: 0;
  color: #736e63;
  font-size: 13px;
}

.favorite-back-button {
  flex-shrink: 0;
  padding: 10px 16px;
  border: 1px solid #e2dcce;
  border-radius: 8px;
  background: #ffffff;
  color: #4a453c;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.favorite-list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 0;
  margin-bottom: 22px;
  border-top: 1px solid #e2dcce;
  border-bottom: 1px solid #e2dcce;
}

.favorite-list-toolbar strong {
  color: #1f1d1a;
  font-size: 15px;
}

.favorite-list-toolbar strong span {
  margin-left: 6px;
  color: #a37e1c;
}

.favorite-page-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 24px 18px;
}

.favorite-page-card {
  position: relative;
  min-width: 0;
}

.favorite-page-poster {
  display: block;
  width: 100%;
  padding: 0;
  aspect-ratio: 2 / 2.85;
  overflow: hidden;
  border: 1px solid #e2dcce;
  border-radius: 10px;
  background: #efece4;
  cursor: pointer;
}

/* 포스터 카드 안의 버튼이 카드 전체 높이를 차지하도록 고정한다. */
.favorite-page-poster > button {
  display: block;
  width: 100%;
  height: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.favorite-page-poster > button:focus-visible {
  outline: 3px solid #a37e1c;
  outline-offset: -3px;
}

.favorite-page-poster img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.favorite-page-poster [role="img"] {
  width: 100%;
  height: 100%;
}

.favorite-page-poster:hover img {
  transform: scale(1.035);
}

.favorite-page-no-poster {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #8c8578;
  font-size: 12px;
}

.favorite-page-movie-info {
  padding-top: 12px;
}

.favorite-page-movie-info h3 {
  margin: 0;
  overflow: hidden;
  color: #1f1d1a;
  font-size: 15px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.favorite-page-movie-info p {
  margin: 6px 0 0;
  color: #736e63;
  font-size: 12px;
}

.favorite-page-empty {
  padding: 64px 20px;
  border: 1px dashed #e2dcce;
  border-radius: 12px;
  background: #ffffff;
  text-align: center;
}

.favorite-empty-symbol {
  margin-bottom: 12px;
  color: #a37e1c;
  font-size: 42px;
}

.favorite-page-empty h2 {
  margin: 0 0 10px;
  font-size: 20px;
}

.favorite-page-empty p {
  margin: 0 0 20px;
  color: #736e63;
  font-size: 13px;
}

.favorite-page-empty button {
  padding: 11px 18px;
  border: 1px solid #e2dcce;
  border-radius: 8px;
  background: #ffd43b;
  color: #594400;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

/* ========================================
   반응형
======================================== */

@media (max-width: 900px) {
  .favorite-page-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .favorite-page-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 20px 12px;
  }

  .favorite-page-heading,
  .favorite-list-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 430px) {
  .favorite-page-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* 관심 영화 페이지 버튼 */

.favorite-back-button,
.favorite-browse-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.favorite-back-button {
  color: #62594e;
  background: #fff;
  border: 1px solid #e2dbd0;
}

.favorite-back-button:hover {
  background: #f5f2eb;
  border-color: #cfc4b5;
}

.favorite-browse-button {
  color: #fff;
  background: #66594b;
  border: 1px solid #66594b;
}

.favorite-browse-button:hover {
  background: #514538;
  border-color: #514538;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(50, 40, 30, 0.12);
}

/* ========================================
   MOMO 대형 관객 평점
======================================== */

/* MOMO 관객 평점 */

.movie-score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  width: 100%;
  min-height: 220px;
  box-sizing: border-box;

  padding: 30px 20px;
  margin: 20px 0;

  background: #f5f2eb;
  border: 1px solid #e2dbd0;
  border-radius: 16px;
}

.movie-score-label {
  font-size: 16px;
  font-weight: 600;
  color: #81796e;
  margin-bottom: 12px;
}

.movie-score-value {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
}

.movie-score-value strong {
  font-size: 72px;
  font-weight: 800;
  line-height: 1;
  color: #b58a45;
}

.movie-score-value span {
  font-size: 22px;
  color: #918779;
}

.movie-score-display p {
  margin: 12px 0 0;
  font-size: 14px;
  color: #81796e;
}

/* =========================
   마이페이지 및 프로필 수정 스타일 정돈
========================= */

/* 1. 마이페이지 프로필 아바타를 동그랗게 정돈 */
.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e5e5e5;
  flex-shrink: 0;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 2. 마이페이지 배경에 들어가는 거대한 슬레이트/클라퍼 아이콘 크기 제한 */
.profile-page::before,
.profile-page .profile-bg,
.profile-page img {
  max-width: 100%;
  max-height: 250px;
  object-fit: contain;
}

/* 3. 프로필 수정 페이지의 아바타 미리보기 영역을 원형으로 고정 */
.profile-image-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  margin: 0 auto 16px auto;
}

.profile-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-image-preview span {
  font-size: 2rem;
  font-weight: bold;
  color: #666;
}

.profile-image-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.profile-image-input-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  margin-bottom: 10px;
}


`;

export default FavoritesAndProfileOverridesStyles;
