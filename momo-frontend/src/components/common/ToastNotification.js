import React, { useEffect, useRef, useState } from "react";
import textlogo from "../../textlogo.png";

function ToastNotification() {
  const [toast, setToast] = useState(null);
  const leaveTimer = useRef(null);
  const removeTimer = useRef(null);

  useEffect(() => {
    const clearTimers = () => {
      window.clearTimeout(leaveTimer.current);
      window.clearTimeout(removeTimer.current);
    };

    const handleToast = (event) => {
      const nickname = event.detail?.nickname?.trim() || "회원";

      clearTimers();
      setToast({ nickname, isLeaving: false });

      leaveTimer.current = window.setTimeout(() => {
        setToast((current) =>
          current ? { ...current, isLeaving: true } : null,
        );
      }, 2700);

      removeTimer.current = window.setTimeout(() => {
        setToast(null);
      }, 3200);
    };

    window.addEventListener("momo-toast", handleToast);

    return () => {
      window.removeEventListener("momo-toast", handleToast);
      clearTimers();
    };
  }, []);

  if (!toast) return null;

  return (
    <div
      className={`momo-toast ${toast.isLeaving ? "is-leaving" : ""}`}
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      <div className="momo-toast-icon" aria-hidden="true">
        <span>✓</span>
      </div>

      <div className="momo-toast-copy">
        <img className="momo-toast-logo" src={textlogo} alt="MOMO" />
        <strong>{toast.nickname}님, 반가워요!</strong>
        <p>오늘도 마음에 남는 영화를 기록해 보세요.</p>
      </div>

      <div className="momo-toast-progress" aria-hidden="true">
        <span />
      </div>
    </div>
  );
}

export default ToastNotification;
