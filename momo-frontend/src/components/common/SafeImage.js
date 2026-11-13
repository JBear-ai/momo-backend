import React, { useEffect, useState } from "react";

// 이미지 주소가 비어 있거나 로딩에 실패하면 화면별 공통 대체 UI를 표시한다.
function SafeImage({
  src,
  alt = "",
  className,
  fallbackClassName,
  fallbackText = "MOMO",
  fallback,
  onError,
  ...imageProps
}) {
  const [failed, setFailed] = useState(!src);

  useEffect(() => {
    setFailed(!src);
  }, [src]);

  if (!src || failed) {
    if (fallback !== undefined) return fallback;

    return React.createElement(
      "div",
      {
        className: fallbackClassName,
        role: "img",
        "aria-label": alt || fallbackText,
      },
      fallbackText,
    );
  }

  return React.createElement("img", {
    ...imageProps,
    src,
    alt,
    className,
    onError: (event) => {
      setFailed(true);
      onError?.(event);
    },
  });
}

export default SafeImage;
