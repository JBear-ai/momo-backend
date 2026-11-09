import React from "react";
import FoundationAndProfileStyles from "./legacy/FoundationAndProfile.styles";
import HomeAndMovieStyles from "./legacy/HomeAndMovie.styles";
import ReviewComposerAuthRecommendStyles from "./legacy/ReviewComposerAuthRecommend.styles";
import ReviewListStyles from "./legacy/ReviewList.styles";
import ReviewDetailAndRatingStyles from "./legacy/ReviewDetailAndRating.styles";
import FavoritesAndProfileOverridesStyles from "./legacy/FavoritesAndProfileOverrides.styles";
import ReviewDetailCommentsStyles from "./legacy/ReviewDetailComments.styles";
import MyReviewsStyles from "./legacy/MyReviews.styles";
import ResponsiveAndPosterOverridesStyles from "./legacy/ResponsiveAndPosterOverrides.styles";

/**
 * App.css를 기능 영역별 styled-components 모듈로 분리한 전역 스타일 번들입니다.
 *
 * CSS 선택자와 DOM className은 기존 호환성을 위해 변경하지 않습니다.
 * 렌더링 순서도 기존 App.css의 선언 순서와 동일하게 유지합니다.
 */
function GlobalStyles() {
  return (
    <>
      <FoundationAndProfileStyles />
      <HomeAndMovieStyles />
      <ReviewComposerAuthRecommendStyles />
      <ReviewListStyles />
      <ReviewDetailAndRatingStyles />
      <FavoritesAndProfileOverridesStyles />
      <ReviewDetailCommentsStyles />
      <MyReviewsStyles />
      <ResponsiveAndPosterOverridesStyles />
    </>
  );
}

export default GlobalStyles;
