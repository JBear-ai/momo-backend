import { createGlobalStyle } from "styled-components";

// 로그인 완료 메시지 — 영화 티켓 + 형광펜 메모 컨셉 (MOMO: MOvie + meMO)
const ToastNotificationStyles = createGlobalStyle`
  .momo-toast {
    /* MOMO 팔레트 — 실제 토큰으로 교체 가능 */
    --momo-paper: #FFFAEC;
    --momo-paper-line: #E9DFC5;
    --momo-ink: #302C24;
    --momo-muted: #827865;
    --momo-mustard: #FFD43B;
    --momo-teal: #FFD43B;
    --momo-teal-deep: #E5B923;
    --momo-stub-width: 78px;

    position: fixed;
    top: calc(24px + env(safe-area-inset-top, 0px));
    right: 24px;
    z-index: 10000;
    display: flex;
    align-items: stretch;
    width: min(400px, calc(100vw - 48px));
    min-height: 92px;
    box-sizing: border-box;
    padding: 0;
    overflow: hidden;
    color: var(--momo-ink);
    font-family: inherit;
    background: var(--momo-paper);
    border: 1px solid var(--momo-paper-line);
    border-radius: 14px;
    box-shadow:
      0 2px 4px rgba(74, 58, 30, 0.08),
      0 18px 36px -8px rgba(74, 58, 30, 0.28);
    transform-origin: top right;
    animation: momo-toast-enter 0.55s cubic-bezier(0.18, 0.9, 0.3, 1.12) both;
  }

  .momo-toast.is-leaving {
    animation: momo-toast-leave 0.38s cubic-bezier(0.5, 0, 0.9, 0.4) both;
    pointer-events: none;
  }

  /* 왼쪽 티켓 스텁 */
  .momo-toast-icon {
    position: relative;
    display: grid;
    flex: 0 0 var(--momo-stub-width);
    align-self: stretch;
    width: var(--momo-stub-width);
    place-items: center;
    color: var(--momo-teal);
    background:
      radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.18), transparent 55%),
      linear-gradient(180deg, var(--momo-teal) 0%, var(--momo-teal-deep) 100%);
    border-radius: 0;
    font-size: 20px;
    font-weight: 900;
  }

  /* 티켓 뜯는 선 (점선) */
  .momo-toast-icon::after {
    content: "";
    position: absolute;
    top: 10px;
    right: -1px;
    bottom: 10px;
    border-right: 2.5px dotted var(--momo-paper);
  }

  .momo-toast-icon span {
    display: grid;
    width: 36px;
    height: 36px;
    place-items: center;
    line-height: 1;
    background: var(--momo-paper);
    border-radius: 50%;
    box-shadow: 0 0 0 4px rgba(255, 250, 236, 0.22);
    animation: momo-toast-check 0.5s 0.2s cubic-bezier(0.2, 1.5, 0.4, 1) both;
  }

  /* 본문 */
  .momo-toast-copy {
    display: grid;
    flex: 1;
    min-width: 0;
    align-content: center;
    gap: 6px;
    padding: 18px 22px 22px 20px;
  }

  .momo-toast-logo {
    display: block;
    width: auto;
    max-width: 96px;
    height: 20px;
    object-fit: contain;
    object-position: left center;
  }

  /* 형광펜 밑줄 */
  .momo-toast-copy strong {
    justify-self: start;
    overflow-wrap: anywhere;
    padding: 0 3px;
    color: var(--momo-ink);
    background: linear-gradient(
      transparent 58%,
      rgba(255, 212, 59, 0.7) 58%,
      rgba(255, 212, 59, 0.7) 92%,
      transparent 92%
    );
    font-size: 17px;
    font-weight: 800;
    letter-spacing: -0.02em;
    line-height: 1.45;
  }

  .momo-toast-copy p {
    margin: 0;
    padding-left: 3px;
    color: var(--momo-muted);
    font-size: 12.5px;
    line-height: 1.55;
  }

  /* 진행 바 — 본문 영역 아래에서만 줄어듭니다 */
  .momo-toast-progress {
    position: absolute;
    right: 0;
    bottom: 0;
    left: var(--momo-stub-width);
    height: 4px;
    overflow: hidden;
    background: var(--momo-paper-line);
  }

  .momo-toast-progress span {
    display: block;
    width: 100%;
    height: 100%;
    background: var(--momo-mustard);
    transform-origin: left center;
    animation: momo-toast-progress 3.2s linear both;
  }

  @keyframes momo-toast-enter {
    0% {
      opacity: 0;
      transform: translate3d(36px, -18px, 0) rotate(3deg) scale(0.94);
    }
    60% {
      opacity: 1;
      transform: translate3d(-3px, 2px, 0) rotate(-0.6deg) scale(1.01);
    }
    100% {
      opacity: 1;
      transform: translate3d(0, 0, 0) rotate(0deg) scale(1);
    }
  }

  @keyframes momo-toast-leave {
    from {
      opacity: 1;
      transform: translate3d(0, 0, 0) rotate(0deg);
    }
    to {
      opacity: 0;
      transform: translate3d(48px, -6px, 0) rotate(2deg);
    }
  }

  @keyframes momo-toast-check {
    from {
      opacity: 0;
      transform: scale(0.3) rotate(-30deg);
    }
    to {
      opacity: 1;
      transform: scale(1) rotate(0deg);
    }
  }

  @keyframes momo-toast-progress {
    from { transform: scaleX(1); }
    to { transform: scaleX(0); }
  }

  @media (max-width: 600px) {
    .momo-toast {
      --momo-stub-width: 64px;

      top: calc(14px + env(safe-area-inset-top, 0px));
      right: 14px;
      left: 14px;
      width: auto;
      min-height: 84px;
    }

    .momo-toast-icon span {
      width: 32px;
      height: 32px;
      font-size: 18px;
    }

    .momo-toast-copy {
      padding: 15px 18px 20px 16px;
    }

    .momo-toast-copy strong {
      font-size: 16px;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .momo-toast,
    .momo-toast.is-leaving,
    .momo-toast-icon span {
      animation-duration: 0.01ms;
    }

    .momo-toast-progress span {
      animation: none;
      transform: scaleX(0);
    }
  }
`;

export default ToastNotificationStyles;
