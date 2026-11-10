import React, { useEffect, useState } from "react";
import AxiosApi from "../api/AxiosApi";
import momoLogo from "../image/std_logo.png";

// 모든 화면에서 동일한 프로필 이미지 조회 및 MOMO 기본 로고 처리를 담당한다.
function ProfileAvatar({
  userId,
  hasImage = false,
  src = "",
  alt = "프로필 이미지",
  className = "",
}) {
  const [loadedImage, setLoadedImage] = useState("");
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    let objectUrl = "";

    setLoadedImage("");
    setFailed(false);

    // 미리보기 이미지가 전달되면 서버 조회를 생략한다.
    if (src || !hasImage || !userId) {
      return () => {
        active = false;
      };
    }

    const loadImage = async () => {
      try {
        const response = await AxiosApi.profileImageByUserIdGet(userId);
        if (!active) return;

        objectUrl = URL.createObjectURL(response.data);
        setLoadedImage(objectUrl);
      } catch (error) {
        if (active) {
          setFailed(true);
          if (error.response?.status !== 404) {
            console.error("프로필 이미지 조회 실패:", error);
          }
        }
      }
    };

    loadImage();

    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [userId, hasImage, src]);

  const imageSource = !failed && (src || loadedImage) ? (src || loadedImage) : momoLogo;

  return (
    <img
      className={className}
      src={imageSource}
      alt={alt}
      onError={() => setFailed(true)}
    />
  );
}

export default ProfileAvatar;
