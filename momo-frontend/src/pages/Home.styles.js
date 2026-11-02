import styled from "styled-components";

/* 전체 홈 화면 */
export const HomePage = styled.div`
  width: 100%;
  color: #292720;
  padding-bottom: 80px;
`;

/* 메인 추천 영역 */
export const FeatureSection = styled.section`
  display: grid;
  grid-template-columns: minmax(280px, 0.85fr) minmax(0, 1.15fr);
  min-height: 440px;
  align-items: stretch;
  margin-bottom: 100px;
  background: #eeeadf;
  overflow: hidden;
  border-radius: 10px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    min-height: auto;
    margin-bottom: 64px;
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
  margin-bottom: 22px;
  color: #927b37;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
`;

/* 메인 제목 */
export const FeatureTitle = styled.h1`
  margin: 0;
  font-size: clamp(34px, 4vw, 58px);
  line-height: 1.15;
  letter-spacing: -0.065em;
  font-weight: 700;
  word-break: keep-all;
`;

/* 메인 설명 */
export const FeatureDescription = styled.p`
  margin: 22px 0 0;
  color: #777164;
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
  background: #d8d2c5;

  /* 추천작 컴포넌트 전체 */
  .recommend-banner {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    min-height: 100%;
    overflow: hidden;
    border-radius: 5px;
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
      rgb(0 0 0 / 72%) 0%,
      rgb(0 0 0 / 35%) 55%,
      rgb(0 0 0 / 8%) 100%
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

  /* 추천작 레이블 */
  .recommend-label {
    display: inline-block;
    margin-bottom: 3px;
    padding: 7px 12px;
    border-radius: 3px;
    background: #f0d66e;
    color: #292720;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.02em;
  }

  .recommend-title {
    margin: 0;
    font-size: clamp(28px, 3.5vw, 44px);
    font-weight: 700;
    line-height: 1.2;
    letter-spacing: -0.05em;
    word-break: keep-all;
  }

  .recommend-info {
    margin: 8px 0 1px;
    color: rgb(255 255 255 / 80%);
    font-size: 14px;
  }

  .recommend-button {
    padding: 10px 18px;
    border: 1px solid rgb(255 255 255 / 65%);
    background: transparent;
    color: #fff;
    font-size: 13px;
    cursor: pointer;
    transition: 0.2s ease;
  }

  .recommend-button:hover {
    border-color: #f0d66e;
    background: #f0d66e;
    color: #292720;
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
    background: #e9e4d9;
    color: #777164;
    font-size: 14px;
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
      font-size: 30px;
    }
  }
`;

/* 공통 섹션 */
export const Section = styled.section`
  margin-bottom: 104px;

  @media (max-width: 768px) {
    margin-bottom: 68px;
  }
`;

/* 섹션 제목 영역 */
export const SectionHeading = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 20px;
  margin-bottom: 28px;

  h2 {
    margin: 0;
    font-size: clamp(24px, 3vw, 34px);
    letter-spacing: -0.055em;
    font-weight: 700;
  }

  p {
    margin: 8px 0 0;
    color: #898477;
    font-size: 13px;
  }
`;

/* 섹션 우측 설명 */
export const MoreText = styled.span`
  color: #777164;
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
    outline: 2px solid #a78b35;
    outline-offset: 5px;
  }
`;

/* 영화 포스터 영역 */
export const PosterWrap = styled.div`
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 2.85;
  overflow: hidden;
  background: #e9e4d9;

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
  color: #fff;
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
  font-style: italic;
  text-shadow: 0 2px 8px rgb(0 0 0 / 45%);
`;

/* 영화 제목 및 개봉일 */
export const MovieInfo = styled.div`
  padding-top: 13px;

  h3 {
    margin: 0;
    overflow: hidden;
    color: #292720;
    font-size: 15px;
    font-weight: 650;
    letter-spacing: -0.035em;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    margin: 7px 0 0;
    color: #898477;
    font-size: 12px;
  }
`;

/* 장르 목록 */
export const GenreList = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  button {
    padding: 13px 22px;
    border: 1px solid #ded8ca;
    border-radius: 999px;
    background: transparent;
    color: #5f594e;
    font-size: 14px;
    cursor: pointer;
    transition: 0.2s ease;
  }

  button:hover {
    border-color: #b69a46;
    background: #f0d66e;
    color: #403719;
  }
`;

/* MOMO 브랜드 소개 */
export const BrandLine = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 30px 0;
  border-top: 1px solid #ded8ca;
  border-bottom: 1px solid #ded8ca;

  h2 {
    margin: 0;
    font-size: clamp(20px, 3vw, 28px);
    letter-spacing: -0.055em;
    font-weight: 600;
  }

  p {
    margin: 8px 0 0;
    color: #898477;
    font-size: 13px;
  }

  strong {
    color: #a78b35;
    font-size: 22px;
    letter-spacing: -0.06em;
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
  color: #9b9486;
  font-size: 12px;
`;
