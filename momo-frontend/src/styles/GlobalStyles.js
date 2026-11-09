import React from "react";
import GlobalResetStyles from "./legacy/GlobalReset.styles";
import FoundationAndProfileStyles from "./legacy/FoundationAndProfile.styles";
import HomeAndMovieStyles from "./legacy/HomeAndMovie.styles";
import ReviewComposerAuthRecommendStyles from "./legacy/ReviewComposerAuthRecommend.styles";
import ReviewListStyles from "./legacy/ReviewList.styles";
import ReviewDetailAndRatingStyles from "./legacy/ReviewDetailAndRating.styles";
import FavoritesAndProfileOverridesStyles from "./legacy/FavoritesAndProfileOverrides.styles";
import ReviewDetailCommentsStyles from "./legacy/ReviewDetailComments.styles";
import MyReviewsStyles from "./legacy/MyReviews.styles";
import ResponsiveAndPosterOverridesStyles from "./legacy/ResponsiveAndPosterOverrides.styles";
import PersonDetailStyles from "./PersonDetail.styles";

/**
 * App.css의 전역 토큰과 기본 스타일 위에 기능별 styled-components를 조립합니다.
 *
 * CSS 선택자와 DOM className은 기존 호환성을 위해 변경하지 않습니다.
 * 렌더링 순서도 기존 App.css의 선언 순서와 동일하게 유지합니다.
 */
function GlobalStyles() {
  return (
    <>
      <GlobalResetStyles />
      <FoundationAndProfileStyles />
      <HomeAndMovieStyles />
      <ReviewComposerAuthRecommendStyles />
      <ReviewListStyles />
      <ReviewDetailAndRatingStyles />
      <FavoritesAndProfileOverridesStyles />
      <ReviewDetailCommentsStyles />
      <MyReviewsStyles />
      <ResponsiveAndPosterOverridesStyles />
      <PersonDetailStyles />
    </>
  );
}

export default GlobalStyles;
