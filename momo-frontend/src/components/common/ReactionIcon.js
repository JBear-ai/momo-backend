import React from "react";
import { ButtonWrapper } from "../../styles/legacy/ReactionIcon.styles";
import likeImg from "../../image/like.png";
import dislikeImg from "../../image/dislike.png";

// 1. 좋아요 버튼 컴포넌트
export function LikeButton({ onClick, count }) {
  return (
    React.createElement(
  ButtonWrapper,
  {"onClick":onClick,"aria-label":"좋아요"},
  React.createElement(
      "img",
      {"src":likeImg,"alt":"좋아요"}
    ),
  (count !== undefined && React.createElement(
      "span",
      null,
      (count)
    ))
)
  );
}

// 2. 싫어요 버튼 컴포넌트
export function DislikeButton({ onClick, count }) {
  return (
    React.createElement(
  ButtonWrapper,
  {"onClick":onClick,"aria-label":"싫어요"},
  React.createElement(
      "img",
      {"src":dislikeImg,"alt":"싫어요"}
    ),
  (count !== undefined && React.createElement(
      "span",
      null,
      (count)
    ))
)
  );
}
