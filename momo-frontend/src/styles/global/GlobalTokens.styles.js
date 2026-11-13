import { createGlobalStyle } from "styled-components";

// MOMO 공통 디자인 토큰과 브라우저 기본 요소의 기본 스타일
const GlobalTokensStyles = createGlobalStyle`
  :root {
    --momo-surface: #FFFFFF;
    --momo-bg: #F5F2EB;
    --momo-border: #E2DBD0;
    --momo-text: #1C1A17;
    --momo-muted: #787062;
    --momo-accent: #FFD43B;
    --momo-accent-ink: #594400;

    --momo-like-bg: #FFF9E6;
    --momo-like-bg-hover: #FFF1C2;
    --momo-like-border: #E6BF33;
    --momo-like-text: #594400;
    --momo-dislike-bg: #FFF1F1;
    --momo-dislike-bg-hover: #FFE2E2;
    --momo-dislike-border: #E7A3A3;
    --momo-dislike-text: #A83232;
    --momo-note: #FFFAEC;
    --momo-note-border: #EFE6D0;
    --momo-shadow: 0 10px 30px -6px rgba(45, 38, 26, 0.08);
    --momo-radius: 14px;
  }

  html,
  body,
  #root {
    min-height: 100%;
  }

  body {
    margin: 0;
    background-color: var(--momo-bg);
    color: var(--momo-text);
    font-family:
      "Pretendard Variable",
      Pretendard,
      -apple-system,
      BlinkMacSystemFont,
      system-ui,
      Roboto,
      "Helvetica Neue",
      "Segoe UI",
      "Apple SD Gothic Neo",
      "Noto Sans KR",
      "Malgun Gothic",
      sans-serif;
    letter-spacing: -0.01em;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  button,
  input,
  textarea,
  select {
    font: inherit;
  }

  code {
    font-family: source-code-pro, Menlo, Monaco, Consolas, "Courier New", monospace;
  }
`;

export default GlobalTokensStyles;
