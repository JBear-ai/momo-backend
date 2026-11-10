import { createGlobalStyle } from "styled-components";

// 기존 className과 선택자를 그대로 유지하는 호환 스타일 모듈입니다.
const ReviewComposerAuthRecommendStyles = createGlobalStyle`
/* 리뷰 영역 스타일 */
/* ========================================
   영화 메모 작성 영역
======================================== */

.review-write-section {
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--momo-note-border);
  border-radius: var(--momo-radius);
  background: var(--momo-note);
}

/* 메모 작성 열기 버튼 */
.review-write-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-height: 64px;
  padding: 18px 24px;
  color: var(--momo-text);
  background: var(--momo-note);
  border: 0;
  cursor: pointer;
  text-align: left;
  font: inherit;
  font-size: 15px;
  font-weight: 800;
  transition: background 0.2s ease;
}

.review-write-toggle:hover {
  background: #F5EDD6;
}

.review-toggle-arrow {
  flex-shrink: 0;
  color: var(--momo-accent-ink);
  font-size: 12px;
}

/* 메모 입력 전체 영역 */
.review-write-content {
  display: grid;
  gap: 24px;
  padding: 28px;
  border-top: 1px solid var(--momo-note-border);
}

.review-write-content h3 {
  margin: 0;
  color: var(--momo-text);
  font-size: 19px;
  font-weight: 800;
  line-height: 1.5;
}

/* 제목 / 본문 입력 영역 */
.review-title-field,
.review-content-field {
  display: grid;
  gap: 10px;
  min-width: 0;
}

/* 입력 항목 이름과 글자 수 */
.review-field-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.review-field-heading label {
  color: var(--momo-text);
  font-size: 14px;
  font-weight: 800;
}

.review-field-heading span {
  flex-shrink: 0;
  color: var(--momo-muted);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 제목 입력창 */
.review-title-field input {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  height: 54px;
  padding: 0 16px;
  color: var(--momo-text);
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 12px;
  outline: none;
  font: inherit;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.review-title-field input:focus,
.review-content-field textarea:focus {
  border-color: var(--momo-accent);
  box-shadow: 0 0 0 3px rgba(255, 212, 59, 0.18);
}

.review-title-field input::placeholder,
.review-content-field textarea::placeholder {
  color: #A39B8E;
}

/* 본문 입력창 */
.review-content-field textarea {
  box-sizing: border-box;
  display: block;
  width: 100%;
  min-width: 0;
  min-height: 220px;
  padding: 18px;
  color: var(--momo-text);
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 12px;
  outline: none;
  font: inherit;
  font-size: 14px;
  line-height: 1.8;
  resize: vertical;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

/* 메모 저장 버튼 */
.review-form-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 2px;
}

.review-form-actions button {
  min-width: 140px;
  min-height: 48px;
  padding: 12px 24px;
  color: var(--momo-accent-ink);
  background: var(--momo-accent);
  border: 0;
  border-radius: 10px;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 212, 59, 0.25);
  transition: transform 0.15s ease, background 0.15s ease;
}

.review-form-actions button:hover:not(:disabled) {
  background: #F5C928;
  transform: translateY(-1px);
}

.review-form-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reaction-buttons {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 6px 0 12px;
}

.reaction-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 52px;
  padding: 10px;
  color: var(--momo-text);
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 8px;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
}

.reaction-button.selected-like {
  background: var(--momo-like-bg);
  border: 2px solid var(--momo-like-border);
  font-weight: 800;
  color: var(--momo-like-text);
}

.reaction-button.selected-dislike {
  background: var(--momo-dislike-bg);
  border: 2px solid var(--momo-dislike-border);
  font-weight: 800;
  color: var(--momo-dislike-text);
}

.review-form-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.review-form-actions button {
  padding: 11px 22px;
  border: none;
  border-radius: 8px;
  background: var(--momo-accent);
  color: var(--momo-accent-ink);
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 212, 59, 0.3);
  transition: transform 0.15s ease;
}

.review-form-actions button:hover {
  transform: translateY(-1px);
}

.detail-review-card {
  padding: 20px;
  margin-top: 14px;
  border: 1px solid var(--momo-border);
  border-radius: 10px;
  background: var(--momo-surface);
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.detail-review-card span {
  margin-left: 12px;
  color: var(--momo-muted);
  font-size: 12px;
}

.signup-wrapper {
  width: 100%;
  max-width: 680px;
  margin: 0 auto;
}

@media (max-width: 900px) {
  .movie-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .movie-detail-header {
    grid-template-columns: minmax(0, 1fr);
    gap: 20px;
    padding: 20px;
  }
}

@media (max-width: 600px) {
  .movie-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}

/* ========================================
   MOMO 회원가입 화면 (.signup-page)
======================================== */
.signup-page {
  width: 100%;
  max-width: 680px;
  margin: 0 auto;
  padding: 50px 20px 80px;
  box-sizing: border-box;
}

.signup-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

.signup-brand {
  display: block;
  width: fit-content;
}

.signup-brand img {
  height: 36px;
  object-fit: contain;
}

.signup-card {
  padding: 36px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: var(--momo-radius);
  box-shadow: var(--momo-shadow);
}

.signup-card-heading {
  padding: 26px;
  margin-bottom: 28px;
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-radius: 10px;
}

.signup-eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--momo-accent-ink);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 1.5px;
}

.signup-card-heading h1 {
  margin: 0 0 10px;
  font-size: clamp(24px, 3vw, 30px);
  font-weight: 800;
  color: var(--momo-text);
  line-height: 1.4;
}

.signup-card-heading p {
  margin: 0 0 16px;
  color: var(--momo-muted);
  font-size: 14px;
  line-height: 1.6;
}

.signup-note-label {
  display: flex;
  gap: 8px;
  font-size: 11px;
  font-weight: 700;
  color: var(--momo-accent-ink);
}

.signup-note-label span {
  padding: 3px 8px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-note-border);
  border-radius: 6px;
}

.signup-card form {
  display: grid;
  gap: 18px;
}

.signup-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.signup-field label {
  font-size: 13px;
  font-weight: 700;
  color: var(--momo-text);
}

.signup-field input {
  padding: 13px 16px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 8px;
  color: var(--momo-text);
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
}

.signup-field input:focus {
  border-color: var(--momo-accent-ink);
  box-shadow: 0 0 0 3px rgba(255, 212, 59, 0.2);
}

.signup-message {
  margin: 2px 0 0;
  font-size: 12px;
  font-weight: 600;
}

.signup-submit {
  width: 100%;
  padding: 15px;
  margin-top: 10px;
  background: var(--momo-accent);
  border: 1px solid var(--momo-border);
  border-radius: 8px;
  color: var(--momo-accent-ink);
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 212, 59, 0.3);
  transition: transform 0.15s ease, filter 0.15s ease;
}

.signup-submit:hover:not(:disabled) {
  filter: brightness(0.96);
  transform: translateY(-1px);
}

.signup-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.signup-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--momo-border);
  font-size: 13px;
  color: var(--momo-muted);
}

.signup-footer a {
  color: var(--momo-text);
  font-weight: 700;
  text-decoration: underline;
}

.signup-bottom-caption {
  margin: 0;
  text-align: center;
  color: var(--momo-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
}

/* ========================================
   오늘의 추천작 배너 (.recommend-banner) - 톤 다운된 감성 버전
======================================== */
.recommend-banner {
    position: relative;
    width: 100%;
    height: 400px;
    overflow: hidden;
    border-radius: var(--momo-radius);
    background-color: #1A1815;
    box-shadow: var(--momo-shadow);
    margin-bottom: 45px;
}

.recommend-poster {
    position: absolute;
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center 25%;
    opacity: 0.85;
}

.recommend-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(
        75deg,
        rgba(20, 18, 15, 0.95) 0%,
        rgba(20, 18, 15, 0.7) 50%,
        rgba(20, 18, 15, 0.2) 100%
    );
}

.recommend-content {
    position: absolute;
    left: 52px;
    bottom: 50px;
    z-index: 1;
    color: white;
    max-width: 65%;
}

.recommend-label {
    display: inline-block;
    margin-bottom: 16px;
    padding: 7px 14px;
    /* 쨍한 노랑 대신 부드럽고 차분한 크림 옐로우 톤 적용 */
    background-color: #F3E9C8;
    color: #4A3800;
    border: 1px solid #E5D7B5;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.5px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.recommend-title {
    margin: 0 0 12px;
    font-size: 38px;
    font-weight: 800;
    line-height: 1.25;
    letter-spacing: -0.02em;
}

.recommend-info {
    margin: 0 0 12px;
    color: rgba(255, 255, 255, 0.85);
    font-size: 15px;
}

.recommend-description {
    max-width: 760px;
    margin: 0 0 26px;
    color: rgba(255, 255, 255, 0.92);
    font-size: 15px;
    line-height: 1.7;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.recommend-button {
    padding: 13px 24px;
    border: 1px solid #E5D7B5;
    border-radius: 8px;
    /* 버튼도 동일하게 차분한 크림 옐로우 톤 적용 */
    background-color: #F3E9C8;
    color: #4A3800;
    font-size: 14px;
    font-weight: 800;
    cursor: pointer;
    box-shadow: 0 6px 16px rgba(0,0,0,0.2);
    transition: transform 0.2s ease, filter 0.2s ease;
}

.recommend-button:hover {
    transform: translateY(-2px);
    filter: brightness(0.96);
}

.recommend-state {
    width: 100%;
    height: 400px;
    margin-bottom: 45px;
    border-radius: var(--momo-radius);
    background-color: var(--momo-surface);
    border: 1px dashed var(--momo-border);
    display: flex;
    justify-content: center;
    align-items: center;
    color: var(--momo-muted);
    font-size: 15px;
    font-weight: 600;
}

/* 반응형 모바일 조정 */
@media (max-width: 900px) {
  .movie-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
    .recommend-banner {
        height: 320px;
    }
    .recommend-content {
        left: 24px;
        bottom: 28px;
        max-width: 85%;
    }
    .recommend-title {
        font-size: 28px;
    }
    .recommend-state {
        height: 320px;
    }
    .movie-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 14px;
    }
}

`;

export default ReviewComposerAuthRecommendStyles;
