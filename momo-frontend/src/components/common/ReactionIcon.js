import React from "react";
import styled from "styled-components";
import likeImg from "../../image/like.png";
import dislikeImg from "../../image/dislike.png";

const ButtonWrapper = styled.button`
  background: none;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  transition: transform 0.1s ease;

  &:hover {
    transform: scale(1.05);
  }

  img {
    width: 20px;
    height: 20px;
    object-fit: contain;
  }

  span {
    font-size: 14px;
    color: #666666;
    font-weight: 500;
  }
`;

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
