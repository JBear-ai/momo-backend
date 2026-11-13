import { createGlobalStyle } from "styled-components";

// 로그인 완료 메시지를 MOMO 공통 디자인으로 표시합니다.
const ToastNotificationStyles = createGlobalStyle`
  .momo-toast {
    position: fixed;
    top: 24px;
    right: 24px;
    z-index: 10000;
    display: flex;
    align-items: center;
    gap: 14px;
    width: min(380px, calc(100vw - 48px));
    box-sizing: border-box;
    padding: 18px 20px 20px;
    overflow: hidden;
    color: #302C24;
    background: #FFFAEC;
    border: 1px solid #E9DFC5;
    border-radius: 16px;
    box-shadow: 0 14px 36px rgba(45, 38, 26, 0.16);
    animation: momo-toast-enter 0.32s ease-out both;
  }

  .momo-toast.is-leaving {
    animation: momo-toast-leave 0.38s ease-in both;
    pointer-events: none;
  }

  .momo-toast-icon {
    display: grid;
    flex: 0 0 42px;
    width: 42px;
    height: 42px;
    place-items: center;
    color: #594400;
    background: #FFD43B;
    border-radius: 13px;
    font-size: 21px;
    font-weight: 900;
  }

  .momo-toast-copy {
    display: grid;
    min-width: 0;
    gap: 5px;
  }

  .momo-toast-logo {
    display: block;
    width: auto;
    max-width: 104px;
    height: 22px;
    object-fit: contain;
    object-position: left center;
  }

  .momo-toast-copy strong {
    overflow-wrap: anywhere;
    color: #302C24;
    font-size: 15px;
    font-weight: 800;
    line-height: 1.45;
  }

  .momo-toast-copy p {
    margin: 0;
    color: #827865;
    font-size: 12px;
    line-height: 1.5;
  }

  .momo-toast-progress {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    height: 3px;
    overflow: hidden;
    background: #EFE6D0;
  }

  .momo-toast-progress span {
    display: block;
    width: 100%;
    height: 100%;
    background: #FFD43B;
    transform-origin: left center;
    animation: momo-toast-progress 3.2s linear both;
  }

  @keyframes momo-toast-enter {
    from {
      opacity: 0;
      transform: translate3d(18px, -8px, 0);
    }
    to {
      opacity: 1;
      transform: translate3d(0, 0, 0);
    }
  }

  @keyframes momo-toast-leave {
    from {
      opacity: 1;
      transform: translate3d(0, 0, 0);
    }
    to {
      opacity: 0;
      transform: translate3d(12px, -4px, 0);
    }
  }

  @keyframes momo-toast-progress {
    from { transform: scaleX(1); }
    to { transform: scaleX(0); }
  }

  @media (max-width: 600px) {
    .momo-toast {
      top: 14px;
      right: 14px;
      left: 14px;
      width: auto;
      padding: 15px 16px 18px;
      border-radius: 14px;
    }

    .momo-toast-icon {
      flex-basis: 38px;
      width: 38px;
      height: 38px;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .momo-toast,
    .momo-toast.is-leaving,
    .momo-toast-progress span {
      animation-duration: 0.01ms;
    }
  }
`;

export default ToastNotificationStyles;
