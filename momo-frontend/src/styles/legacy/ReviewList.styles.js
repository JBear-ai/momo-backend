import { createGlobalStyle } from "styled-components";

// 기존 className과 선택자를 그대로 유지하는 호환 스타일 모듈입니다.
const ReviewListStyles = createGlobalStyle`
/* ========================================
   MOMO 전체 리뷰 페이지 (.all-reviews-page)
======================================== */

.all-reviews-page {
  width: 100%;
  max-width: 1160px;
  margin: 0 auto;
  padding: 30px 0 80px;
  color: var(--momo-text);
}

/* 상단 헤더 */
.all-reviews-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 36px;
  margin-bottom: 32px;
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-radius: var(--momo-radius);
  box-shadow: var(--momo-shadow);
  position: relative;
  overflow: hidden;
}

.all-reviews-heading::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 6px;
  background: var(--momo-accent);
}

.all-reviews-eyebrow {
  margin: 0 0 10px;
  color: var(--momo-accent-ink);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.5px;
}

.all-reviews-heading h1 {
  margin: 0 0 10px;
  font-size: clamp(26px, 4vw, 34px);
  font-weight: 800;
  letter-spacing: -0.03em;
}

.all-reviews-description {
  margin: 0;
  color: var(--momo-muted);
  font-size: 14px;
  line-height: 1.7;
}

.all-reviews-count {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 120px;
  height: 100px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-note-border);
  border-radius: 12px;
}

.all-reviews-count span {
  margin-bottom: 7px;
  color: var(--momo-muted);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1px;
}

.all-reviews-count strong {
  color: var(--momo-accent-ink);
  font-size: 20px;
  font-weight: 900;
}

/* 리뷰 목록 카드 */
.all-reviews-card {
  padding: 30px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: var(--momo-radius);
  box-shadow: var(--momo-shadow);
}

.all-reviews-card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--momo-border);
}

.all-reviews-card-heading h2 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 800;
}

.all-reviews-card-heading p {
  margin: 0;
  color: var(--momo-muted);
  font-size: 13px;
}

.all-reviews-page-info {
  flex-shrink: 0;
  padding: 8px 12px;
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-radius: 7px;
  color: var(--momo-accent-ink);
  font-size: 12px;
  font-weight: 700;
}

/* 전체 리뷰 검색 */
.all-reviews-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(220px, 2fr) auto auto;
  align-items: center;
  gap: 10px;
  margin: 0 0 24px;
  padding: 16px;
  background: #FFFCF4;
  border: 1px solid var(--momo-note-border);
  border-radius: 10px;
}

.all-reviews-search-label {
  color: var(--momo-muted);
  font-size: 12px;
  font-weight: 750;
  white-space: nowrap;
}

.all-reviews-search-select,
.all-reviews-search-input {
  width: 100%;
  min-width: 0;
  height: 42px;
  padding: 0 12px;
  color: var(--momo-text);
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 7px;
  outline: none;
  font-size: 13px;
}

.all-reviews-search-select:focus,
.all-reviews-search-input:focus {
  border-color: var(--momo-accent-ink);
  box-shadow: 0 0 0 3px rgba(255, 212, 59, 0.18);
}

.all-reviews-search-submit,
.all-reviews-search-reset {
  height: 42px;
  padding: 0 16px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 750;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.all-reviews-search-submit {
  color: var(--momo-accent-ink);
  background: var(--momo-accent);
  border: 1px solid var(--momo-accent);
}

.all-reviews-search-submit:hover {
  background: #F5C928;
}

.all-reviews-search-reset {
  color: var(--momo-muted);
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
}

.all-reviews-search-reset:hover {
  background: var(--momo-bg);
  border-color: #BEB5A7;
}

/* 리뷰 테이블 */
.all-reviews-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.all-reviews-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.all-reviews-table thead {
  background: var(--momo-note);
}

.all-reviews-table th {
  padding: 15px 12px;
  color: var(--momo-muted);
  font-size: 12px;
  font-weight: 800;
  text-align: left;
  border-top: 1px solid var(--momo-note-border);
  border-bottom: 1px solid var(--momo-note-border);
}

.all-reviews-table td {
  padding: 18px 12px;
  border-bottom: 1px solid var(--momo-border);
  font-size: 13px;
  vertical-align: middle;
}

.review-movie-column {
  width: 27%;
}

.review-title-column {
  width: 43%;
}

.review-author-column {
  width: 15%;
}

.review-date-column {
  width: 15%;
}

.review-movie-cell {
  overflow-wrap: anywhere;
}

.review-movie-title {
  font-weight: 750;
}

.review-movie-year {
  margin-left: 5px;
  color: var(--momo-muted);
  font-size: 12px;
}

.review-title-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-title-link {
  color: var(--momo-text);
  font-weight: 700;
  text-decoration: none;
  transition: color 0.15s ease;
}

.review-title-link:hover {
  color: var(--momo-accent-ink);
  text-decoration: underline;
}

.review-author-cell,
.review-date-cell {
  color: var(--momo-muted);
  font-size: 12px !important;
}

.all-reviews-table tbody tr {
  transition: background 0.15s ease;
}

.all-reviews-table tbody tr:hover {
  background: #FFFCF4;
}

/* 로딩 / 오류 / 빈 목록 */
.all-reviews-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  padding: 30px;
  color: var(--momo-muted);
  background: var(--momo-bg);
  border: 1px dashed var(--momo-border);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}

.all-reviews-error {
  color: #C0392B;
  background: #FFF5F5;
  border-color: #EED2D2;
}

/* 모바일 리뷰 목록 */
.all-reviews-mobile-list {
  display: none;
}

.all-reviews-mobile-item {
  padding: 18px 4px;
  border-bottom: 1px solid var(--momo-border);
}

.mobile-review-movie {
  margin: 0 0 9px;
  color: var(--momo-muted);
  font-size: 12px;
  font-weight: 700;
}

.mobile-review-title {
  display: block;
  margin-bottom: 12px;
  color: var(--momo-text);
  font-size: 15px;
  font-weight: 800;
  line-height: 1.5;
  text-decoration: none;
  overflow-wrap: anywhere;
}

.mobile-review-title:hover {
  color: var(--momo-accent-ink);
}

.mobile-review-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--momo-muted);
  font-size: 11px;
}

/* 페이지네이션 */
.all-reviews-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 30px;
}

.all-reviews-pagination button {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 7px;
  color: var(--momo-text);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.all-reviews-pagination button:hover:not(:disabled) {
  background: var(--momo-note);
  border-color: var(--momo-accent-ink);
}

.all-reviews-pagination button.active {
  background: var(--momo-accent);
  border-color: var(--momo-accent);
  color: var(--momo-accent-ink);
  font-weight: 900;
}

.all-reviews-pagination button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* 반응형 */
@media (max-width: 900px) {
  .all-reviews-search {
    grid-template-columns: 1fr 2fr;
  }

  .all-reviews-search-label {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .all-reviews-page {
    padding: 20px 0 50px;
  }

  .all-reviews-heading {
    padding: 28px 22px;
  }

  .all-reviews-count {
    width: 95px;
    height: 85px;
  }

  .all-reviews-card {
    padding: 22px 16px;
  }
}

@media (max-width: 600px) {
  .all-reviews-search {
    grid-template-columns: 1fr 1fr;
    padding: 12px;
  }

  .all-reviews-search-select,
  .all-reviews-search-input {
    grid-column: 1 / -1;
  }

  .all-reviews-search-submit,
  .all-reviews-search-reset {
    width: 100%;
  }

  .all-reviews-heading {
    padding: 26px 20px;
  }

  .all-reviews-count {
    display: none;
  }

  .all-reviews-card-heading {
    align-items: flex-start;
  }

  .all-reviews-table-wrap {
    display: none;
  }

  .all-reviews-mobile-list {
    display: block;
  }

  .all-reviews-pagination {
    gap: 5px;
  }

  .all-reviews-pagination button {
    min-width: 32px;
    height: 34px;
    padding: 0 8px;
  }
}

`;

export default ReviewListStyles;
