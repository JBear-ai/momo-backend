import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import "../App.css";
import AxiosApi from "../api/AxiosApi";

/* =========================
   정규식
========================= */

// 이메일: 이메일 형식 검사
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

// 비밀번호: 영문, 숫자, 특수문자 포함 8~20자
const PWD_REGEX =
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/;

// 닉네임: 한글, 영문, 숫자 2~10자
const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9]{2,10}$/;

function ProfileEdit() {
  const navigate = useNavigate();

  // 비밀번호 검증 요청의 순서를 관리
  const passwordCheckRequestId = useRef(0);
  const profileImageUrlRef = useRef("");
  const profilePreviewRef = useRef("");

  /* =========================
     회원정보 상태
  ========================= */

  const [userId, setUserId] = useState("");
  const [nickname, setNickname] = useState("");
  const [email, setEmail] = useState("");

  /* =========================
     프로필 이미지 상태
  ========================= */

  // 현재 저장된 프로필 이미지 경로
  const [profileImage, setProfileImage] = useState("");

  // 서버에서 불러온 프로필 이미지 표시 주소
  const [profileImageUrl, setProfileImageUrl] = useState("");

  // 새로 선택한 이미지 파일
  const [profileFile, setProfileFile] = useState(null);

  // 선택한 이미지의 미리보기 주소
  const [profilePreview, setProfilePreview] = useState("");

  // 프로필 이미지 업로드 진행 상태
  const [profileImageUploading, setProfileImageUploading] = useState(false);

  /* =========================
     비밀번호 상태
  ========================= */

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // 현재 비밀번호 검증 결과
  const [passwordCheckResult, setPasswordCheckResult] = useState(null);

  // 현재 비밀번호 검증 진행 상태
  const [passwordChecking, setPasswordChecking] = useState(false);

  // 현재 비밀번호 검증 오류
  const [passwordCheckError, setPasswordCheckError] = useState("");

  // 저장 진행 상태
  const [isSubmitting, setIsSubmitting] = useState(false);

  /* =========================
     기존 회원정보 불러오기
  ========================= */

  useEffect(() => {
    let active = true;

    async function loadProfile() {
      try {
        const userResponse = await AxiosApi.memberMe();
        const user = userResponse.data || {};

        if (!active) return;

        // 서버 회원정보를 우선 사용하고 기존 로컬 값은 누락 필드의 임시 대체값으로만 사용한다.
        setUserId(user.loginId || user.username || localStorage.getItem("username") || "");
        setNickname(user.nickname || localStorage.getItem("nickname") || "");
        setEmail(user.email || localStorage.getItem("email") || "");

        const savedProfileImage = user.profileImage || "";
        setProfileImage(savedProfileImage);

        if (savedProfileImage) {
          const imageResponse = await AxiosApi.profileImageGet();
          if (!active) return;
          const objectUrl = URL.createObjectURL(imageResponse.data);
          profileImageUrlRef.current = objectUrl;
          setProfileImageUrl(objectUrl);
        }
      } catch (error) {
        if (!active) return;
        console.error("회원정보 및 프로필 이미지 조회 실패:", error);
        if (error.response?.status === 401) {
          navigate("/login", { replace: true });
        }
      }
    }

    loadProfile();

    return () => {
      active = false;
      if (profileImageUrlRef.current) {
        URL.revokeObjectURL(profileImageUrlRef.current);
        profileImageUrlRef.current = "";
      }
      if (profilePreviewRef.current) {
        URL.revokeObjectURL(profilePreviewRef.current);
        profilePreviewRef.current = "";
      }
    };
  }, [navigate]);

  /* =========================
     입력값 유효성 검사
  ========================= */

  // 닉네임 검사
  const isNickname = NICKNAME_REGEX.test(nickname.trim());

  // 이메일 검사
  const isEmail = EMAIL_REGEX.test(email.trim());

  // 새 비밀번호 정규식 검사
  const isPwd = PWD_REGEX.test(newPassword);

  // 새 비밀번호 확인 검사
  const isPwdCheck = confirmPassword !== "" && newPassword === confirmPassword;

  // 비밀번호 변경 여부
  const isPasswordChange =
    currentPassword !== "" || newPassword !== "" || confirmPassword !== "";

  // 기존 비밀번호와 새 비밀번호 동일 여부
  const isSamePassword =
    currentPassword !== "" &&
    newPassword !== "" &&
    currentPassword === newPassword;

  // 비밀번호 변경 유효성
  const isPasswordValid =
    !isPasswordChange ||
    (currentPassword !== "" &&
      newPassword !== "" &&
      isPwd &&
      isPwdCheck &&
      !isSamePassword);

  // 전체 유효성
  const isAllValid =
    isNickname && isEmail && isPasswordValid && !isSubmitting && !profileImageUploading;

  /* =========================
     현재 비밀번호 검증
  ========================= */

  const handleCurrentPasswordCheck = async () => {
    const password = currentPassword;

    // 이전에 실행된 요청과 구분하기 위한 번호
    const requestId = ++passwordCheckRequestId.current;

    // 현재 비밀번호가 비어 있으면 검증하지 않음
    if (!password) {
      setPasswordCheckResult(null);
      setPasswordCheckError("");
      setPasswordChecking(false);
      return null;
    }

    try {
      setPasswordChecking(true);
      setPasswordCheckResult(null);
      setPasswordCheckError("");

      // 백엔드에 현재 비밀번호 검증 요청
      const response = await AxiosApi.memberPasswordCheck(password);

      // 최신 요청의 결과만 반영
      const isMatched = response.data === true;

      if (requestId === passwordCheckRequestId.current) {
        setPasswordCheckResult(isMatched);
      }

      return isMatched;
    } catch (error) {
      console.error("현재 비밀번호 검증 실패:", error);

      // 최신 요청의 오류만 반영
      if (requestId === passwordCheckRequestId.current) {
        setPasswordCheckResult(null);
        setPasswordCheckError(
          "현재 비밀번호 확인에 실패했습니다. 다시 시도해 주세요.",
        );
      }

      return null;
    } finally {
      if (requestId === passwordCheckRequestId.current) {
        setPasswordChecking(false);
      }
    }
  };

  /* =========================
     현재 비밀번호 입력 변경
  ========================= */

  const handleCurrentPasswordChange = (event) => {
    // 진행 중인 이전 검증 요청 무효화
    passwordCheckRequestId.current += 1;

    setCurrentPassword(event.target.value);

    // 기존 검증 결과 초기화
    setPasswordCheckResult(null);
    setPasswordCheckError("");
    setPasswordChecking(false);
  };

  /* =========================
      프로필 이미지 변경
    ========================= */

  const handleProfileImageChange = (event) => {
    const file = event.target.files?.[0];

    // 파일을 선택하지 않은 경우에는 처리하지 않습니다.
    if (!file) {
      return;
    }

    // JPG, JPEG, PNG 이미지 파일만 허용합니다.
    const allowedTypes = ["image/jpeg", "image/png"];

    if (!allowedTypes.includes(file.type)) {
      alert("JPG, JPEG, PNG 이미지 파일만 선택할 수 있습니다.");
      event.target.value = "";
      return;
    }

    // 프로필 이미지의 최대 크기는 5MB로 제한합니다.
    if (file.size > 5 * 1024 * 1024) {
      alert("프로필 이미지는 5MB 이하만 선택할 수 있습니다.");
      event.target.value = "";
      return;
    }

    setProfileFile(file);

    // 이전 미리보기 주소가 있으면 먼저 정리합니다.
    if (profilePreviewRef.current) {
      URL.revokeObjectURL(profilePreviewRef.current);
    }

    // 사용자가 선택한 이미지를 업로드 전에 미리 보여줍니다.
    const previewUrl = URL.createObjectURL(file);
    profilePreviewRef.current = previewUrl;
    setProfilePreview(previewUrl);
  };

  // 선택한 프로필 이미지를 서버에 업로드합니다.
  const handleProfileImageUpload = async () => {
    if (!profileFile) {
      alert("업로드할 프로필 이미지를 선택해 주세요.");
      return;
    }

    try {
      setProfileImageUploading(true);

      const response = await AxiosApi.profileImageUpload(profileFile);

      // 서버에 저장된 새 프로필 이미지 경로를 상태에 반영합니다.
      setProfileImage(response.data.profileImage || "");

      // 업로드된 실제 이미지를 다시 불러와 화면에 표시합니다.
      const imageResponse = await AxiosApi.profileImageGet();
      const newImageUrl = URL.createObjectURL(imageResponse.data);

      // 기존 이미지 주소가 있으면 정리한 뒤 새 이미지 주소로 변경합니다.
      if (profileImageUrlRef.current) {
        URL.revokeObjectURL(profileImageUrlRef.current);
      }
      profileImageUrlRef.current = newImageUrl;
      setProfileImageUrl(newImageUrl);

      // 선택한 파일과 미리보기 상태를 초기화합니다.
      if (profilePreviewRef.current) {
        URL.revokeObjectURL(profilePreviewRef.current);
        profilePreviewRef.current = "";
      }

      setProfileFile(null);
      setProfilePreview("");

      alert("프로필 이미지가 변경되었습니다.");
    } catch (error) {
      console.error("프로필 이미지 업로드 실패:", error);

      const errorMessage =
        error.response?.data?.message ||
        "프로필 이미지 업로드 중 오류가 발생했습니다.";

      alert(errorMessage);
    } finally {
      setProfileImageUploading(false);
    }
  };

  // 저장된 프로필 이미지를 삭제하고 기본 아바타로 되돌립니다.
  const handleProfileImageDelete = async () => {
    try {
      setProfileImageUploading(true);

      await AxiosApi.profileImageDelete();

      // 기존에 생성한 이미지 주소를 정리합니다.
      if (profileImageUrlRef.current) {
        URL.revokeObjectURL(profileImageUrlRef.current);
        profileImageUrlRef.current = "";
      }

      if (profilePreviewRef.current) {
        URL.revokeObjectURL(profilePreviewRef.current);
        profilePreviewRef.current = "";
      }

      setProfileImage("");
      setProfileImageUrl("");
      setProfileFile(null);
      setProfilePreview("");

      alert("프로필 이미지가 삭제되었습니다.");
    } catch (error) {
      console.error("프로필 이미지 삭제 실패:", error);

      const errorMessage =
        error.response?.data?.message ||
        "프로필 이미지 삭제 중 오류가 발생했습니다.";

      alert(errorMessage);
    } finally {
      setProfileImageUploading(false);
    }
  };

  /* =========================
     회원정보 수정
  ========================= */

  const handleSave = async (event) => {
    event.preventDefault();

    // 중복 요청 방지
    if (isSubmitting) return;

    /* =========================
       닉네임 검사
    ========================= */

    if (!nickname.trim()) {
      alert("닉네임을 입력해 주세요.");
      return;
    }

    if (!isNickname) {
      alert("닉네임은 한글, 영문, 숫자로 2~10자 입력해 주세요.");
      return;
    }

    /* =========================
       이메일 검사
    ========================= */

    if (!email.trim()) {
      alert("이메일을 입력해 주세요.");
      return;
    }

    if (!isEmail) {
      alert("올바른 이메일 형식을 입력해 주세요.");
      return;
    }

    /* =========================
       비밀번호 변경 검사
    ========================= */

    if (isPasswordChange) {
      // 현재 비밀번호 입력 여부
      if (!currentPassword) {
        alert("현재 비밀번호를 입력해 주세요.");
        return;
      }

      // 새 비밀번호 입력 여부
      if (!newPassword) {
        alert("새 비밀번호를 입력해 주세요.");
        return;
      }

      // 새 비밀번호 정규식
      if (!isPwd) {
        alert(
          "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자로 입력해 주세요.",
        );
        return;
      }

      // 기존 비밀번호와 동일한지 검사
      if (isSamePassword) {
        alert("새 비밀번호는 기존 비밀번호와 다르게 설정해 주세요.");
        return;
      }

      // 비밀번호 확인 입력 여부
      if (!confirmPassword) {
        alert("새 비밀번호 확인을 입력해 주세요.");
        return;
      }

      // 비밀번호 일치 여부
      if (!isPwdCheck) {
        alert("새 비밀번호가 일치하지 않습니다.");
        return;
      }
    }

    /* =========================
       서버 요청
    ========================= */

    try {
      setIsSubmitting(true);

      // 비밀번호 변경을 요청한 경우 현재 비밀번호 검증
      if (isPasswordChange) {
        // 이미 불일치로 확인된 경우
        if (passwordCheckResult === false) {
          alert("현재 비밀번호가 일치하지 않습니다.");
          return;
        }

        // 아직 검증하지 않았거나 검증 결과가 없는 경우
        if (passwordCheckResult !== true) {
          const isCurrentPasswordValid = await handleCurrentPasswordCheck();

          if (isCurrentPasswordValid !== true) {
            if (isCurrentPasswordValid === false) {
              alert("현재 비밀번호가 일치하지 않습니다.");
            } else {
              alert("현재 비밀번호 확인에 실패했습니다. 다시 시도해 주세요.");
            }

            return;
          }
        }
      }

      // 회원정보 수정 API 요청
      await AxiosApi.memberUpdate({
        nickname: nickname.trim(),
        email: email.trim(),

        // 비밀번호 변경을 하지 않으면 null 전달
        currentPassword: isPasswordChange ? currentPassword : null,

        newPassword: isPasswordChange ? newPassword : null,
      });

      // 수정 성공 후 로컬 정보 갱신
      localStorage.setItem("nickname", nickname.trim());
      localStorage.setItem("email", email.trim());

      alert("프로필 정보가 수정되었습니다.");

      navigate("/profile", { replace: true });
    } catch (error) {
      console.error("회원정보 수정 실패:", error);

      const errorMessage =
        error.response?.data?.message ||
        (typeof error.response?.data === "string"
          ? error.response.data
          : "회원정보 수정 중 오류가 발생했습니다.");

      alert(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  /* =========================
     화면
  ========================= */

  return (
    <main className="profile-page">
      <section className="profile-container">
        {/* 페이지 제목 */}
        <header className="profile-heading">
          <h1>프로필 수정</h1>
          <p>회원 정보를 수정할 수 있습니다.</p>
        </header>

        <section className="profile-card">
          <form className="profile-form" onSubmit={handleSave}>
            {/* 프로필 이미지 */}
            <div className="profile-image-edit">
              <div className="profile-image-preview">
                {profilePreview || profileImageUrl ? (
                  <img src={profilePreview || profileImageUrl} alt="프로필" />
                ) : (
                  <span>
                    {(nickname || userId || "M").charAt(0).toUpperCase()}
                  </span>
                )}
              </div>

              <input
                id="profile-image-input"
                type="file"
                accept="image/jpeg,image/png"
                onChange={handleProfileImageChange}
                disabled={profileImageUploading || isSubmitting}
              />

              <div className="profile-image-buttons">
                <button
                  type="button"
                  className="profile-button"
                  onClick={handleProfileImageUpload}
                  disabled={!profileFile || profileImageUploading || isSubmitting}
                >
                  {profileImageUploading ? "처리 중..." : "이미지 변경"}
                </button>

                {(profileImage || profileImageUrl) && (
                  <button
                    type="button"
                    className="profile-button profile-button-secondary"
                    onClick={handleProfileImageDelete}
                    disabled={profileImageUploading || isSubmitting}
                  >
                    이미지 삭제
                  </button>
                )}
              </div>
            </div>
            {/* 아이디 */}
            <label htmlFor="edit-userId">아이디</label>

            <input id="edit-userId" type="text" value={userId} readOnly />

            {/* 닉네임 */}
            <label htmlFor="edit-nickname">닉네임</label>

            <input
              id="edit-nickname"
              type="text"
              value={nickname}
              onChange={(event) => setNickname(event.target.value)}
              required
            />

            <p
              className="signup-message"
              style={{
                color: !nickname ? "#888" : isNickname ? "#2e9e4f" : "#e5484d",
              }}
            >
              {!nickname
                ? ""
                : isNickname
                  ? "사용 가능한 닉네임 형식입니다."
                  : "한글, 영문, 숫자로 2~10자 입력하세요."}
            </p>

            {/* 이메일 */}
            <label htmlFor="edit-email">이메일</label>

            <input
              id="edit-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="이메일을 입력해 주세요."
              required
            />

            <p
              className="signup-message"
              style={{
                color: !email ? "#888" : isEmail ? "#2e9e4f" : "#e5484d",
              }}
            >
              {!email
                ? ""
                : isEmail
                  ? "올바른 이메일 형식입니다."
                  : "이메일 형식이 올바르지 않습니다."}
            </p>

            {/* 구분선 */}
            <hr
              style={{
                margin: "24px 0",
                border: "0",
                borderTop: "1px solid #eee",
              }}
            />

            {/* 비밀번호 변경 제목 */}
            <h3
              style={{
                marginBottom: "16px",
                fontSize: "1.1rem",
              }}
            >
              비밀번호 변경
            </h3>

            <p
              style={{
                fontSize: "13px",
                color: "#888",
                marginBottom: "16px",
              }}
            >
              비밀번호를 변경하지 않으려면 아래 항목을 비워 두세요.
            </p>

            {/* 현재 비밀번호 */}
            <label htmlFor="current-password">현재 비밀번호</label>

            <input
              id="current-password"
              type="password"
              value={currentPassword}
              onChange={handleCurrentPasswordChange}
              onBlur={handleCurrentPasswordCheck}
              placeholder="현재 비밀번호를 입력해 주세요."
              autoComplete="current-password"
            />

            {/* 현재 비밀번호 검증 결과 */}
            <p
              className="signup-message"
              style={{
                color: passwordCheckError
                  ? "#e5484d"
                  : passwordCheckResult === true
                    ? "#2e9e4f"
                    : passwordCheckResult === false
                      ? "#e5484d"
                      : "#888",
              }}
            >
              {passwordChecking
                ? "현재 비밀번호 확인 중입니다."
                : passwordCheckError
                  ? passwordCheckError
                  : passwordCheckResult === true
                    ? "현재 비밀번호가 일치합니다."
                    : passwordCheckResult === false
                      ? "현재 비밀번호가 일치하지 않습니다."
                      : ""}
            </p>

            {/* 새 비밀번호 */}
            <label htmlFor="new-password">새 비밀번호</label>

            <input
              id="new-password"
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              placeholder="새 비밀번호를 입력해 주세요."
              autoComplete="new-password"
            />

            <p
              className="signup-message"
              style={{
                color: !newPassword
                  ? "#888"
                  : isSamePassword
                    ? "#e5484d"
                    : isPwd
                      ? "#2e9e4f"
                      : "#e5484d",
              }}
            >
              {!newPassword
                ? ""
                : isSamePassword
                  ? "기존 비밀번호와 다른 비밀번호를 입력해 주세요."
                  : isPwd
                    ? "사용 가능한 비밀번호입니다."
                    : "영문, 숫자, 특수문자를 포함해 8~20자로 입력하세요."}
            </p>

            {/* 새 비밀번호 확인 */}
            <label htmlFor="confirm-password">새 비밀번호 확인</label>

            <input
              id="confirm-password"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              placeholder="새 비밀번호를 다시 입력해 주세요."
              autoComplete="new-password"
            />

            <p
              className="signup-message"
              style={{
                color: !confirmPassword
                  ? "#888"
                  : isPwdCheck
                    ? "#2e9e4f"
                    : "#e5484d",
              }}
            >
              {!confirmPassword
                ? ""
                : isPwdCheck
                  ? "비밀번호가 일치합니다."
                  : "비밀번호가 일치하지 않습니다."}
            </p>

            {/* 버튼 */}
            <div className="profile-form-buttons" style={{ marginTop: "24px" }}>
              <button
                type="submit"
                className="profile-button"
                disabled={!isAllValid || profileImageUploading}
              >
                {isSubmitting ? "저장 중..." : "저장"}
              </button>

              <button
                type="button"
                className="profile-button profile-button-secondary"
                onClick={() => navigate("/profile")}
                disabled={isSubmitting}
              >
                취소
              </button>
            </div>
          </form>
        </section>
      </section>
    </main>
  );
}

export default ProfileEdit;
