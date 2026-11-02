import styled from "styled-components";

/* 전체 홈 화면: 포근하고 깨끗한 다이어리 미색 배경 */
export const HomePage = styled.div`
  width: 100%;
  background-color: #f8f6f0;
  color: #2d2a26;
  padding-bottom: 80px;
`;

/* 메인 추천 영역: 배경과 부드럽게 이어지는 고급 메모지 카드 톤 */
export const FeatureSection = styled.section`
  display: grid;
  grid-template-columns: minmax(280px, 0.85fr) minmax(0, 1.15fr);
  min-height: 440px;
  align-items: stretch;
  margin-bottom: 80px;
  background: #ffffff;
  overflow: hidden;
  border-radius: 16px;
  border: 1px solid #e6e1d6;
  box-shadow: 0 12px 32px -8px rgba(50, 44, 32, 0.05);

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    min-height: auto;
    margin-bottom: 54px;
  }
`;

/* 왼쪽 소개 영역 */
export const FeatureInfo = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px 48px;

  @media (max-width: 768px) {
    padding: 36px 24px;
  }
`;

/* 상단 작은 문구 */
export const Eyebrow = styled.span`
  display: block;
  margin-bottom: 18px;
  color: #a37e1c; /* 차분하고 고급스러운 빈티지 골드 */
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.15em;
  text-transform: uppercase;
`;

/* 메인 제목 */
export const FeatureTitle = styled.h1`
  margin: 0;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.2;
  letter-spacing: -0.04em;
  font-weight: 800;
  color: #1f1d1a;
  word-break: keep-all;
`;

/* 메인 설명 */
export const FeatureDescription = styled.p`
  margin: 20px 0 0;
  color: #736e63;
  font-size: 15px;
  line-height: 1.8;
  word-break: keep-all;
`;

/* 오늘의 추천작 배너 영역 */
export const FeatureVisual = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 440px;
  overflow: hidden;
  background: #efece4;

  /* 추천작 컴포넌트 전체 */
  .recommend-banner {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    min-height: 100%;
    overflow: hidden;
    border-radius: 0;
  }

  /* 영화 포스터 */
  .recommend-poster {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    min-height: 100%;
    display: block;
    object-fit: cover;
    object-position: center 25%;
    transition: transform 0.6s ease;
  }

  /* 배너 어두운 오버레이 */
  .recommend-overlay {
    position: absolute;
    inset: 0;
    z-index: 1;
    background: linear-gradient(
      90deg,
      rgba(15, 14, 12, 0.88) 0%,
      rgba(15, 14, 12, 0.55) 55%,
      rgba(15, 14, 12, 0.15) 100%
    );
  }

  /* 추천 영화 정보 */
  .recommend-content {
    position: absolute;
    z-index: 2;
    left: 40px;
    bottom: 44px;
    width: calc(100% - 80px);
    color: #fff;
  }

  /* 오늘의 추천작 레이블 - 쨍한 노랑을 빼고 차분한 크림 옐로우로 고정 */
  .recommend-label {
    display: inline-block;
    margin-bottom: 8px;
    padding: 6px 12px;
    border-radius: 6px;
    background: #f3e9c8;
    color: #4a3800;
    border: 1px solid #e5d7b5;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.02em;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  }

  .recommend-title {
    margin: 0;
    font-size: clamp(26px, 3.2vw, 40px);
    font-weight: 800;
    line-height: 1.25;
    letter-spacing: -0.03em;
    word-break: keep-all;
  }

  .recommend-info {
    margin: 8px 0 14px;
    color: rgba(255, 255, 255, 0.8);
    font-size: 14px;
  }

  /* 오늘의 추천작 버튼 - 쨍한 노랑을 빼고 차분한 크림 옐로우로 고정 */
  .recommend-button {
    padding: 11px 20px;
    border: 1px solid #e5d7b5;
    background: #f3e9c8;
    color: #4a3800;
    font-size: 13px;
    font-weight: 800;
    border-radius: 8px;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    transition: all 0.2s ease;
  }

  .recommend-button:hover {
    filter: brightness(0.95);
    transform: translateY(-1px);
  }

  /* 로딩 및 오류 화면 */
  .recommend-state {
    position: absolute;
    inset: 0;
    z-index: 3;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: #faf8f2;
    color: #736e63;
    font-size: 14px;
    font-weight: 600;
  }

  /* 포스터 확대 효과 */
  &:hover .recommend-poster {
    transform: scale(1.025);
  }

  /* 모바일 */
  @media (max-width: 768px) {
    min-height: 340px;
    height: 340px;

    .recommend-content {
      left: 24px;
      bottom: 28px;
      width: calc(100% - 48px);
    }

    .recommend-title {
      font-size: 26px;
    }
  }
`;

/* 공통 섹션 */
export const Section = styled.section`
  margin-bottom: 90px;

  @media (max-width: 768px) {
    margin-bottom: 60px;
  }
`;

/* 섹션 제목 영역 */
export const SectionHeading = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 20px;
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: clamp(22px, 2.8vw, 30px);
    letter-spacing: -0.04em;
    font-weight: 800;
    color: #1f1d1a;
  }

  p {
    margin: 6px 0 0;
    color: #736e63;
    font-size: 13px;
  }
`;

/* 섹션 우측 설명 */
export const MoreText = styled.span`
  color: #8c8578;
  font-size: 13px;
  white-space: nowrap;
`;

/* 박스오피스 영화 목록 */
export const MovieGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 24px 18px;

  @media (max-width: 1000px) {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  @media (max-width: 700px) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 20px 12px;
  }

  @media (max-width: 430px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
`;

/* 영화 카드 */
export const MovieCard = styled.button`
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;

  &:hover img {
    transform: scale(1.035);
  }

  &:focus-visible {
    outline: 2px solid #a37e1c;
    outline-offset: 5px;
  }
`;

/* 영화 포스터 영역 */
export const PosterWrap = styled.div`
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 2.85;
  overflow: hidden;
  background: #efece4;
  border-radius: 10px;
  border: 1px solid #e2dcce;
  box-shadow: 0 4px 12px rgba(50, 44, 32, 0.04);

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
    transition: transform 0.35s ease;
  }
`;

/* 영화 순위 */
export const Rank = styled.span`
  position: absolute;
  left: 12px;
  bottom: 8px;
  color: #ffffff;
  font-size: 32px;
  line-height: 1;
  font-weight: 900;
  font-style: italic;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.65);
`;

/* 영화 제목 및 개봉일 */
export const MovieInfo = styled.div`
  padding-top: 12px;

  h3 {
    margin: 0;
    overflow: hidden;
    color: #1f1d1a;
    font-size: 15px;
    font-weight: 750;
    letter-spacing: -0.02em;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    margin: 6px 0 0;
    color: #736e63;
    font-size: 12px;
  }
`;

/* 장르 목록 */
export const GenreList = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  button {
    padding: 11px 20px;
    border: 1px solid #e2dcce;
    border-radius: 999px;
    background: #ffffff;
    color: #4a453c;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 2px 5px rgba(50, 44, 32, 0.02);
    transition: all 0.2s ease;
  }

  button:hover {
    border-color: #a37e1c;
    background: #ffd43b;
    color: #4a3800;
  }
`;

/* MOMO 브랜드 소개 */
export const BrandLine = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 36px 0;
  border-top: 1px solid #e2dcce;
  border-bottom: 1px solid #e2dcce;

  h2 {
    margin: 0;
    font-size: clamp(20px, 3vw, 26px);
    letter-spacing: -0.04em;
    font-weight: 800;
    color: #1f1d1a;
  }

  p {
    margin: 6px 0 0;
    color: #736e63;
    font-size: 13px;
  }

  @media (max-width: 600px) {
    align-items: flex-start;
    flex-direction: column;
  }
`;

/* 포스터가 없는 경우 */
export const EmptyPoster = styled.div`
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #8c8578;
  font-size: 12px;
  font-weight: 600;
`;
