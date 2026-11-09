import { createGlobalStyle } from "styled-components";

// 기존 className과 선택자를 그대로 유지하는 호환 스타일 모듈입니다.
const FoundationAndProfileStyles = createGlobalStyle`
/* ========================================
   MOMO Design System - "영화를 메모하다, 가볍고 따뜻한 기록"
======================================== */
:root {
  --momo-surface: #FFFFFF;
  --momo-bg: #F5F2EB;              /* 은은한 인디 아이보리 베이스 톤 */
  --momo-border: #E2DBD0;          /* 손으로 그린 듯 부드러운 경계선 */
  --momo-text: #1C1A17;            /* 가독성이 돋보이는 딥 잉크 톤 */
  --momo-muted: #787062;           /* 차분한 서브 텍스트 그레이 */
  --momo-accent: #FFD43B;          /* 선명하고 따뜻한 시그니처 메모지 옐로우 */
  --momo-accent-ink: #594400;      /* 포인트 텍스트 및 강조 잉크 컬러 */
  --momo-note: #FFFAEC;            /* 실제 메모지 배경색 (미색) */
  --momo-note-border: #EFE6D0;     /* 메모지 테두리선 */
  --momo-shadow: 0 10px 30px -6px rgba(45, 38, 26, 0.08); /* 깊이감 있는 세련된 그림자 */
  --momo-radius: 14px;
}

/* 전체 바디 및 공통 설정 */
body {
  background-color: var(--momo-bg);
  color: var(--momo-text);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  letter-spacing: -0.01em;
}

/* ========================================
   마이페이지 / 프로필 수정 / 회원 탈퇴 스타일
======================================== */
.profile-page {
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
  padding: 30px 20px 80px;
}

.profile-container {
  display: grid;
  gap: 24px;
}

.profile-heading h1 {
  margin: 0 0 6px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--momo-text);
}

.profile-heading p {
  margin: 0;
  color: var(--momo-muted);
  font-size: 14px;
}

.profile-eyebrow {
  margin-bottom: 8px;
  color: var(--momo-accent-ink);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}

.profile-card {
  padding: 28px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: var(--momo-radius);
  box-shadow: var(--momo-shadow);
  position: relative;
}

/* 메모지 감성 포인트 라인 */
.profile-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 28px;
  right: 28px;
  height: 2px;
  background: linear-gradient(90deg, var(--momo-accent) 0%, transparent 100%);
  border-radius: 2px;
}

.profile-card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.profile-card-heading h2 {
  margin: 0 0 4px;
  font-size: 19px;
  font-weight: 750;
  color: var(--momo-text);
}

.profile-card-heading p {
  margin: 0;
  color: var(--momo-muted);
  font-size: 13px;
}

.profile-card-actions {
  display: flex;
  gap: 8px;
}

.profile-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 18px;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 8px;
  color: var(--momo-text);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 5px rgba(0,0,0,0.02);
}

.profile-button:hover {
  background: var(--momo-note);
  border-color: var(--momo-accent-ink);
  transform: translateY(-1px);
}

.profile-button-secondary {
  background: var(--momo-note);
  border-color: var(--momo-note-border);
}

.profile-delete-button {
  background: #FFF5F5;
  border-color: #EED2D2;
  color: #C53030;
  text-decoration: none;
}

/* 회원 탈퇴 버튼 - 활성화 상태에서만 호버 적용 (추가 / 11.02 )*/
.profile-delete-button:not(:disabled):hover {
  background: #fed7d7;
}

/* 체크박스 선택 전 - 비활성화 */
.profile-delete-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 체크박스 선택 후 - 활성화 */
.profile-delete-button:not(:disabled) {
  opacity: 1;
  cursor: pointer;
}

.profile-user {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  background: var(--momo-accent);
  border: 2px solid var(--momo-surface);
  box-shadow: 0 4px 12px rgba(255, 212, 59, 0.3);
  border-radius: 50%;
  font-size: 26px;
  font-weight: 800;
  color: var(--momo-accent-ink);
  flex-shrink: 0;
}

.profile-user-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.profile-user-info h3 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
}

.profile-user-info p {
  display: flex;
  justify-content: space-between;
  margin: 0;
  font-size: 13px;
  color: var(--momo-text);
  border-bottom: 1px dashed var(--momo-border);
  padding-bottom: 6px;
}

.profile-user-info span {
  color: var(--momo-muted);
  font-weight: 600;
}

/* 영화 활동 섹션 */
.profile-activity {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.profile-activity-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  background: var(--momo-note);
  border: 1px solid var(--momo-note-border);
  border-radius: 10px;
  transition: transform 0.2s ease;
}

.profile-activity-item:hover {
  transform: translateY(-2px);
}

.profile-activity-icon {
  font-size: 18px;
}

.profile-activity-item h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}

.profile-activity-item p {
  margin: 0;
  color: var(--momo-muted);
  font-size: 12px;
}

.profile-coming {
  display: inline-block;
  margin-top: auto;
  padding: 3px 8px;
  width: fit-content;
  background: var(--momo-surface);
  border: 1px solid var(--momo-border);
  border-radius: 6px;
  font-size: 11px;
  color: var(--momo-muted);
  font-weight: 600;
}

.profile-delete-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.profile-form {
  display: grid;
  gap: 12px;
}

.profile-form label {
  font-size: 13px;
  font-weight: 700;
}

.profile-form input {
  padding: 13px 16px;
  border: 1px solid var(--momo-border);
  border-radius: 8px;
  font-size: 14px;
  background: var(--momo-surface);
  color: var(--momo-text);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.profile-form input:read-only {
  background: var(--momo-bg);
  color: var(--momo-muted);
}

.profile-form input:focus {
  border-color: var(--momo-accent-ink);
  box-shadow: 0 0 0 3px rgba(255, 212, 59, 0.2);
  outline: none;
}

.profile-form-buttons {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.profile-delete-check {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0;
  font-size: 14px;
  cursor: pointer;
}

@media (max-width: 600px) {
  .profile-activity {
    grid-template-columns: 1fr;
  }
  .profile-delete-section {
    flex-direction: column;
    align-items: flex-start;
  }
}

`;

export default FoundationAndProfileStyles;
