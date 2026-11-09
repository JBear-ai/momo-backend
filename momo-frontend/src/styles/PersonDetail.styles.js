import { createGlobalStyle } from "styled-components";

/**
 * PersonDetail 화면 전용 스타일입니다.
 * PersonDetail.js의 기존 className을 그대로 사용하며,
 * 영화 상세 페이지의 공통 스타일과 충돌하지 않도록 필모그래피 영역을 직접 지정합니다.
 */
const PersonDetailStyles = createGlobalStyle`
.movie-detail-page .detail-rank {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--momo-accent-ink);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.movie-detail-page .person-meta-details {
  display: grid;
  gap: 4px;
  margin: 12px 0;
  line-height: 1.6;
}

.movie-detail-page .person-meta-details p {
  margin: 0;
  color: var(--momo-muted);
  font-size: 14px;
  line-height: 1.6;
}

.movie-detail-page .movie-detail-info > p {
  margin: 0;
  color: var(--momo-muted);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-line;
}

.movie-detail-page > .detail-content-section > .people-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  align-items: stretch;
  gap: 20px;
}

.movie-detail-page > .detail-content-section > .people-list > .person-chip {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  padding: 12px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid var(--momo-border);
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
  cursor: default;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.movie-detail-page > .detail-content-section > .people-list > .person-chip:hover {
  background: #ffffff;
  color: var(--momo-text);
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.movie-detail-page > .detail-content-section > .people-list > .person-chip > img {
  display: block;
  width: 100%;
  height: 250px;
  object-fit: cover;
  border-radius: 6px;
}

.movie-detail-page > .detail-content-section > .people-list > .person-chip > div:first-child {
  display: flex;
  width: 100%;
  height: 250px;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  border-radius: 6px;
  font-size: 32px;
}

.movie-detail-page > .detail-content-section > .people-list > .person-chip > div:last-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.movie-detail-page > .detail-content-section > .people-list > .person-chip strong {
  font-size: 15px;
  line-height: 1.3;
  overflow-wrap: anywhere;
}

.movie-detail-page > .detail-content-section > .people-list > .person-chip > div:last-child > span {
  color: #666666;
  font-size: 13px;
}

.movie-detail-page > .detail-content-section > .people-list > .person-chip > div:last-child > .person-role {
  width: fit-content;
  padding-left: 0;
  border-left: 0;
  color: #444444;
  font-size: 13px;
}

.movie-detail-page > .detail-content-section > .people-list + div {
  margin-top: 30px;
  text-align: center;
}

.movie-detail-page > .detail-content-section > .people-list + div > button {
  padding: 12px 24px;
  background: #f9f9f9;
  border: 1px solid #cccccc;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.movie-detail-page > .detail-content-section > .people-list + div > button:hover {
  background: #f0f0f0;
  border-color: #aaaaaa;
}

@media (max-width: 600px) {
  .movie-detail-page > .detail-content-section {
    padding: 22px 16px;
  }

  .movie-detail-page > .detail-content-section > .people-list {
    grid-template-columns: repeat(auto-fill, minmax(145px, 1fr));
    gap: 12px;
  }

  .movie-detail-page > .detail-content-section > .people-list > .person-chip > img,
  .movie-detail-page > .detail-content-section > .people-list > .person-chip > div:first-child {
    height: 210px;
  }
}
`;

export default PersonDetailStyles;
