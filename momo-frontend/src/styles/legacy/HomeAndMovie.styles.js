import { createGlobalStyle } from "styled-components";

// 기존 className과 선택자를 그대로 유지하는 호환 스타일 모듈입니다.
const HomeAndMovieStyles = createGlobalStyle`
/* ========================================
   MOMO 홈 화면 (.home-page)
======================================== */
.home-page {
  width: 100%;
}

.home-page .hero-banner {
  padding: clamp(28px, 5vw, 44px);
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-radius: var(--momo-radius);
  box-shadow: var(--momo-shadow);
  position: relative;
  overflow: hidden;
}

/* 히어로 배너 왼쪽 포인트 바 */
.home-page .hero-banner::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  background-color: var(--momo-accent);
}

.home-page .hero-content h1 {
  font-size: clamp(27px, 4vw, 38px);
  line-height: 1.45;
  font-weight: 800;
}

.home-page .hero-description {
  color: var(--momo-muted);
  line-height: 1.8;
}

.home-page .eyebrow {
  margin-bottom: 12px;
  color: var(--momo-accent-ink);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1.5px;
}

.home-page .section-block {
  margin: 40px 0;
  scroll-margin-top: 90px;
}

.home-page .section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.home-page .section-heading h2 {
  margin-bottom: 4px;
  font-size: 22px;
  font-weight: 800;
}

.home-page .section-description {
  margin-bottom: 0;
  color: var(--momo-muted);
}

.home-page .home-search {
  max-width: 420px;
  margin-bottom: 20px;
}

.home-page .state-message,
.home-page .empty-review-panel {
  padding: 28px;
  color: var(--momo-muted);
  background: var(--momo-surface);
  border: 1px dashed var(--momo-border);
  border-radius: var(--momo-radius);
  text-align: center;
  font-weight: 600;
}

.home-page .movie-info h3 {
  margin: 12px 0 5px;
  overflow-wrap: anywhere;
}

.home-page .movie-info p {
  margin: 0;
  color: var(--momo-muted);
  font-size: 13px;
}

.home-page .movie-info .outline-button,
.home-page .movie-info .like-button {
  width: 100%;
  margin-top: 8px;
}

.home-page .like-button {
  background: var(--momo-note);
}

.home-page .review-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.home-page .community-review {
  min-width: 0;
  padding: 22px;
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-left: 5px solid var(--momo-accent);
  border-radius: 8px 12px 12px 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.02);
  overflow-wrap: anywhere;
}

.home-page .community-review p {
  white-space: pre-wrap;
  margin: 8px 0 12px;
}

.home-page .community-review small {
  color: var(--momo-muted);
}

.home-page .modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(45, 38, 26, 0.4);
  backdrop-filter: blur(2px);
}

.home-page .review-modal {
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 30px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: var(--momo-radius);
  box-shadow: 0 16px 40px rgba(45, 38, 26, 0.15);
}

.home-page .modal-close {
  display: flex;
  margin-left: auto;
}

.home-page .review-modal form {
  display: grid;
  gap: 12px;
}

.home-page .review-modal label {
  font-weight: 700;
}

.home-page .review-modal textarea {
  min-height: 120px;
}

/* ========================================
   영화 카드 및 그리드 목록 (.movie-grid, .movie-card)
======================================== */
.movie-search-input {
  width: 100%;
  max-width: 420px;
  padding: 14px 18px;
  margin: 24px 0;
  border: 1px solid var(--momo-border);
  border-radius: 10px;
  background: var(--momo-surface);
  color: var(--momo-text);
  font-size: 14px;
  outline: none;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
  transition: all 0.2s ease;
}

.movie-search-input:focus {
  border-color: var(--momo-accent-ink);
  box-shadow: 0 0 0 3px rgba(255, 212, 59, 0.2);
}

.movie-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
  width: 100%;
  max-width: 1160px;
  margin: 0 auto;
}

.movie-grid > * {
  min-width: 0;
  max-width: 100%;
}

.movie-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  padding: 14px;
  color: inherit;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: var(--momo-radius);
  box-shadow: var(--momo-shadow);
  text-decoration: none;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.25s ease;
}

.movie-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 40px -10px rgba(45, 39, 28, 0.14);
  border-color: var(--momo-accent-ink);
}

.movie-card img {
  width: 100%;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  background: #EAE3D2;
  border-radius: 8px;
}

.movie-card h3 {
  margin: 12px 0 5px;
  font-size: 16px;
  font-weight: 800;
  color: var(--momo-text);
  word-break: keep-all;
}

.movie-card p {
  margin-bottom: 0;
  color: var(--momo-muted);
  font-size: 13px;
}

.movie-card .movie-info {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  gap: 5px;
}

.movie-release-date,
.movie-audience-info p {
  margin: 6px 0;
  color: var(--momo-muted);
  font-size: 12px;
}

.movie-audience-info {
  margin: 12px 0;
}

.movie-audience-info strong {
  color: var(--momo-text);
}

.movie-review-button,
.movie-like-button {
  width: 100%;
  padding: 11px;
  margin-top: 8px;
  border: 1px solid var(--momo-note-border);
  border-radius: 8px;
  background: var(--momo-note);
  color: var(--momo-text);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.movie-review-button:hover,
.movie-like-button:hover {
  background: var(--momo-accent);
  color: var(--momo-accent-ink);
}

.poster-frame {
  position: relative;
  overflow: hidden;
  aspect-ratio: 2 / 3;
  background: #EAE3D2;
  border-radius: 8px;
}

.poster-frame .movie-poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.poster-rank {
  position: absolute;
  z-index: 2;
  top: 10px;
  left: 10px;
  padding: 5px 10px;
  color: var(--momo-accent-ink);
  background: var(--momo-accent);
  border: 1px solid rgba(0,0,0,0.05);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 800;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.poster-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  min-height: 220px;
  aspect-ratio: 2 / 3;
  padding: 12px;
  color: var(--momo-muted);
  background: #EAE3D2;
  border: 1px dashed var(--momo-border);
  border-radius: 8px;
  text-align: center;
  overflow-wrap: anywhere;
}

.placeholder-mark {
  font-size: 36px;
  font-weight: 800;
}

.movie-message {
  padding: 30px 10px;
  color: var(--momo-muted);
  text-align: center;
  font-weight: 600;
}

.error-message {
  color: #C0392B;
}

/* ========================================
   MOMO 영화 상세 페이지 (.movie-detail-page)
======================================== */
.movie-detail-page {
  width: 100%;
  max-width: 1160px;
  margin: 0 auto;
  padding-bottom: 60px;
  color: var(--momo-text);
}

.detail-back-button {
  padding: 11px 18px;
  margin: 10px 0 24px;
  border: 1px solid var(--momo-border);
  border-radius: 8px;
  background: var(--momo-surface);
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 5px rgba(0,0,0,0.02);
  transition: background 0.15s ease;
}

.detail-back-button:hover {
  background: var(--momo-note);
}

.movie-detail-header {
  display: grid;
  grid-template-columns: minmax(240px, 320px) minmax(0, 1fr);
  gap: 36px;
  padding: 32px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: var(--momo-radius);
  box-shadow: var(--momo-shadow);
}

.movie-detail-poster {
  width: 100%;
  overflow: hidden;
  aspect-ratio: 2 / 3;
  background: #EAE3D2;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}

.movie-detail-poster img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.movie-detail-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.movie-detail-info h1 {
  margin: 0 0 8px;
  font-size: clamp(25px, 3vw, 34px);
  overflow-wrap: anywhere;
  font-weight: 800;
}

.movie-detail-info > p:not(.eyebrow) {
  margin: 0;
  color: var(--momo-muted);
  font-size: 14px;
}

.movie-rating-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  margin-top: 18px;
  padding: 18px 20px;
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-radius: 10px;
}

.rating-label {
  color: var(--momo-muted);
  font-size: 12px;
  font-weight: 700;
}

.movie-rating-number {
  color: var(--momo-text);
  font-size: 36px;
  font-weight: 900;
  margin: 6px 0;
}

.movie-rating-panel small {
  color: var(--momo-muted);
  font-size: 12px;
}

.detail-reaction-counts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 10px;
}

.detail-reaction-count {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 8px;
}

.detail-reaction-count span {
  font-size: 13px;
  color: var(--momo-muted);
}

.detail-reaction-count strong {
  font-size: 18px;
}

.detail-content-section {
  margin-top: 36px;
  padding: 32px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: var(--momo-radius);
  box-shadow: var(--momo-shadow);
}

.detail-content-section > h2 {
  margin: 0 0 22px;
  font-size: 22px;
  font-weight: 800;
}

.people-group + .people-group {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--momo-border);
}

.people-group h3 {
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 750;
}

.people-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.person-chip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 15px;
  color: var(--momo-text);
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.person-chip:hover {
  background: var(--momo-accent);
  color: var(--momo-accent-ink);
}

.person-chip > span {
  color: var(--momo-accent-ink);
  font-size: 11px;
}

.person-role {
  padding-left: 8px;
  border-left: 1px solid var(--momo-note-border);
  color: var(--momo-muted);
  font-size: 12px;
}

.people-empty {
  margin: 0;
  padding: 16px;
  color: var(--momo-muted);
  background: var(--momo-bg);
  border: 1px dashed var(--momo-border);
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
}

.person-filmography {
  margin-top: 24px;
  padding: 24px;
  border-left: 5px solid var(--momo-accent);
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-radius: var(--momo-radius);
}

.filmography-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding-left: 0;
  list-style: none;
}

.filmography-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-note-border);
  border-radius: 8px;
}

.filmography-item strong {
  min-width: 0;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.filmography-item span {
  flex-shrink: 0;
  color: var(--momo-muted);
  font-size: 12px;
}

`;

export default HomeAndMovieStyles;
