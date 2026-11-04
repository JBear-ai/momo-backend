import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";
import logoImage from "../textlogo.png";

function Login() {
  const [inputId, setInputId] = useState("");
  const [inputPwd, setInputPwd] = useState("");

  const nav = useNavigate();

  // 아이디와 비밀번호가 모두 입력되었는지 확인
  const isValid = inputId.trim() !== "" && inputPwd !== "";

  // 로그인 버튼 클릭 또는 Enter 입력 시 실행
  const onSubmit = async (e) => {
    e.preventDefault();

    try {
      // [수정 완료] 두 번째 인자에 inputPwd가 들어가도록 수정!
      const response = await AxiosApi.memberLogin(inputId, inputPwd);

      console.log("로그인 응답 데이터:", response.data);

      if (response.status === 200 && response.data) {
        const nickname = response.data.nickname || "회원";
        const loginId = response.data.loginId || inputId.trim();

        // 로그인 성공 후 기존 사용자 정보 저장
        localStorage.setItem("username", loginId);
        localStorage.setItem("nickname", nickname);

        // 로그인 성공 후 Layout에 로그인 상태가 변경되었음을 알림
        // Layout은 이벤트를 받으면 서버에 로그인 상태를 다시 확인
        window.dispatchEvent(new Event("momo-auth-changed"));

        alert("로그인에 성공했습니다.");
        nav("/", { replace: true });
      } else {
        alert("아이디 또는 비밀번호가 틀립니다.");
      }
    } catch (error) {
      console.error("로그인 에러:", error);
      alert("아이디 또는 비밀번호가 올바르지 않습니다.");
    }
  };

  return (
    <main className="signup-page">
      <section className="signup-wrapper">
        <Link to="/" className="signup-brand">
          <img src={logoImage} alt="MOMO 로고" />
        </Link>

        <section className="signup-card login-card">
          <div className="signup-card-heading">
            <span className="signup-eyebrow">WELCOME BACK</span>

            <h1>
              다시, 나만의 영화 메모로
              <br />
              돌아오셨네요.
            </h1>

            <p>
              로그인하고 마음에 남는 영화와
              <br />
              다양한 리뷰를 만나 보세요.
            </p>

            <div className="signup-note-label">
              <span>MY MOVIE NOTE</span>
              <span>LOGIN TO MOMO</span>
            </div>
          </div>

          <form onSubmit={onSubmit}>
            <div className="signup-field">
              <label htmlFor="loginId">아이디</label>
              <input
                id="loginId"
                type="text"
                value={inputId}
                placeholder="아이디를 입력해 주세요."
                onChange={(e) => setInputId(e.target.value)}
              />
            </div>

            <div className="signup-field">
              <label htmlFor="loginPassword">비밀번호</label>
              <input
                id="loginPassword"
                type="password"
                value={inputPwd}
                placeholder="비밀번호를 입력해 주세요."
                onChange={(e) => setInputPwd(e.target.value)}
              />
            </div>

            <button type="submit" className="signup-submit" disabled={!isValid}>
              로그인
              <span>→</span>
            </button>
          </form>

          <div className="signup-footer">
            <span>아직 계정이 없으신가요?</span>
            <Link to="/signup">회원가입</Link>
          </div>
        </section>

        <p className="signup-bottom-caption">YOUR MOVIES, YOUR MEMORIES.</p>
      </section>
    </main>
  );
}

export default Login;
