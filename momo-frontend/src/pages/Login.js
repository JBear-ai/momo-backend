import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AxiosApi from "../api/AxiosApi";
import logoImage from "../textlogo.png";

function Login() {
  const [inputId, setInputId] = useState("");
  const [inputPwd, setInputPwd] = useState("");
  const navigate = useNavigate();

  // 아이디와 비밀번호가 입력되었는지 확인
  const isValid = inputId.trim() !== "" && inputPwd !== "";

  // 로그인 요청
  const onSubmit = async (event) => {
    event.preventDefault();

    try {
      const response = await AxiosApi.memberLogin(inputId, inputPwd);

      if (response.status === 200 && response.data) {
        const loginId = response.data.loginId || inputId.trim();
        const nickname = response.data.nickname || "회원";

        localStorage.setItem("username", loginId);
        localStorage.setItem("nickname", nickname);

        // 로그인 상태가 바뀌었다는 것을 Layout에 알린다.
        window.dispatchEvent(new Event("momo-auth-changed"));

        alert("로그인에 성공했습니다.");
        navigate("/", { replace: true });
      } else {
        alert("아이디 또는 비밀번호가 틀립니다.");
      }
    } catch (error) {
      console.error("로그인 에러:", error);
      alert("아이디 또는 비밀번호가 올바르지 않습니다.");
    }
  };

  return (
    React.createElement(
  "main",
  {"className":"signup-page"},
  React.createElement(
      "section",
      {"className":"signup-wrapper"},
      React.createElement(
            Link,
            {"to":"/","className":"signup-brand"},
            React.createElement(
                    "img",
                    {"src":logoImage,"alt":"MOMO 로고"}
                  )
          ),
      React.createElement(
            "section",
            {"className":"signup-card login-card"},
            React.createElement(
                    "div",
                    {"className":"signup-card-heading"},
                    React.createElement(
                              "span",
                              {"className":"signup-eyebrow"},
                              "WELCOME BACK"
                            ),
                    React.createElement(
                              "h1",
                              null,
                              "다시, 나만의 영화 메모로",
                              React.createElement(
                                          "br",
                                          null
                                        ),
                              "돌아오셨네요."
                            ),
                    React.createElement(
                              "p",
                              null,
                              "로그인하고 마음에 남는 영화와",
                              React.createElement(
                                          "br",
                                          null
                                        ),
                              "다양한 리뷰를 만나 보세요."
                            ),
                    React.createElement(
                              "div",
                              {"className":"signup-note-label"},
                              React.createElement(
                                          "span",
                                          null,
                                          "MY MOVIE NOTE"
                                        ),
                              React.createElement(
                                          "span",
                                          null,
                                          "LOGIN TO MOMO"
                                        )
                            )
                  ),
            React.createElement(
                    "form",
                    {"onSubmit":onSubmit},
                    React.createElement(
                              "div",
                              {"className":"signup-field"},
                              React.createElement(
                                          "label",
                                          {"htmlFor":"loginId"},
                                          "아이디"
                                        ),
                              React.createElement(
                                          "input",
                                          {"id":"loginId","type":"text","value":inputId,"placeholder":"아이디를 입력해 주세요.","onChange":(event) => setInputId(event.target.value)}
                                        )
                            ),
                    React.createElement(
                              "div",
                              {"className":"signup-field"},
                              React.createElement(
                                          "label",
                                          {"htmlFor":"loginPassword"},
                                          "비밀번호"
                                        ),
                              React.createElement(
                                          "input",
                                          {"id":"loginPassword","type":"password","value":inputPwd,"placeholder":"비밀번호를 입력해 주세요.","onChange":(event) => setInputPwd(event.target.value)}
                                        )
                            ),
                    React.createElement(
                              "button",
                              {"type":"submit","className":"signup-submit","disabled":!isValid},
                              "로그인",
                              React.createElement(
                                          "span",
                                          null,
                                          "→"
                                        )
                            )
                  ),
            React.createElement(
                    "div",
                    {"className":"signup-footer"},
                    React.createElement(
                              "span",
                              null,
                              "아직 계정이 없으신가요?"
                            ),
                    React.createElement(
                              Link,
                              {"to":"/signup"},
                              "회원가입"
                            )
                  )
          ),
      React.createElement(
            "p",
            {"className":"signup-bottom-caption"},
            "YOUR MOVIES, YOUR MEMORIES."
          )
    )
)
  );
}

export default Login;
