import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import logoImage from "../textlogo.png";
import AxiosApi from "../api/AxiosApi";

/* 정규식 */

// 이메일: 이메일 형식 검사
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

// 비밀번호: 영문, 숫자, 특수문자 포함 8~20자
const PWD_REGEX =
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/;

// 닉네임: 한글, 영문, 숫자 2~10자
const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9]{2,10}$/;

function SignUp() {
  // 입력값
  const [inputId, setInputId] = useState("");
  const [inputEmail, setInputEmail] = useState("");
  const [inputPwd, setInputPwd] = useState("");
  const [inputPwdCheck, setInputPwdCheck] = useState("");
  const [inputNickname, setInputNickname] = useState("");

  // 아이디 중복 확인 상태
  const [idStatus, setIdStatus] = useState("idle");

  const nav = useNavigate();

  // 입력값 유효성 검사
  const isId = inputId.trim() !== "";
  const isEmail = EMAIL_REGEX.test(inputEmail);
  const isPwd = PWD_REGEX.test(inputPwd);
  const isPwdCheck = inputPwdCheck !== "" && inputPwd === inputPwdCheck;
  const isNickname = NICKNAME_REGEX.test(inputNickname);

  /*
   * 아이디 중복 확인
   * 입력이 멈춘 뒤 0.5초 후 서버에 요청
   */
  useEffect(() => {
    if (!isId) {
      setIdStatus("idle");
      return;
    }

    setIdStatus("checking");

    let ignore = false;

    const timer = setTimeout(async () => {
      try {
        const response = await AxiosApi.memberIdExist(inputId.trim());

        if (ignore) return;

        // 이미 존재하는 아이디면 true
        setIdStatus(response.data === true ? "duplicate" : "available");
      } catch (error) {
        if (!ignore) {
          setIdStatus("error");
        }
      }
    }, 500);

    return () => {
      ignore = true;
      clearTimeout(timer);
    };
  }, [inputId, isId]);

  // 아이디 중복 확인 통과 + 나머지 입력값 유효성 검사
  const isIdOk = isId && idStatus === "available";

  const isAllValid = isIdOk && isEmail && isPwd && isPwdCheck && isNickname;

  // 아이디 메시지
  const getIdMessage = () => {
    if (!inputId) return "";

    switch (idStatus) {
      case "checking":
        return "아이디 중복 확인 중...";
      case "available":
        return "사용 가능한 아이디입니다.";
      case "duplicate":
        return "이미 사용 중인 아이디입니다.";
      case "error":
        return "아이디 중복 확인 중 오류가 발생했습니다.";
      default:
        return "";
    }
  };

  // 회원가입 요청
  const onSubmit = async (e) => {
    e.preventDefault();

    if (!isAllValid) return;

    try {
      const response = await AxiosApi.memberSignup(
        inputId.trim(),
        inputPwd,
        inputNickname.trim(),
        inputEmail.trim(),
      );
      // 백엔드에서 HTTP 201을 반환하면 회원가입 성공
      if (response.status === 201) {
        alert(`${inputNickname}님, 회원가입이 완료되었습니다.`);
        nav("/login", { replace: true });
      } else {
        alert("회원가입에 실패했습니다.");
      }
    } catch (error) {
      console.error("회원가입 오류:", error);
      alert("회원가입 중 오류가 발생했습니다.");
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
            {"className":"signup-card"},
            React.createElement(
                    "div",
                    {"className":"signup-card-heading"},
                    React.createElement(
                              "span",
                              {"className":"signup-eyebrow"},
                              "YOUR MOVIE JOURNAL"
                            ),
                    React.createElement(
                              "h1",
                              null,
                              "나만의 영화 메모를",
                              React.createElement(
                                          "br",
                                          null
                                        ),
                              "시작해 볼까요?"
                            ),
                    React.createElement(
                              "p",
                              null,
                              "회원가입하고 마음에 남는 영화와",
                              React.createElement(
                                          "br",
                                          null
                                        ),
                              "다양한 리뷰를 기록해 보세요."
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
                                          "JOIN MOMO"
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
                                          {"htmlFor":"signupId"},
                                          "아이디"
                                        ),
                              React.createElement(
                                          "input",
                                          {"id":"signupId","type":"text","name":"id","value":inputId,"placeholder":"아이디를 입력해 주세요.","onChange":(e) => setInputId(e.target.value),"required":true}
                                        ),
                              React.createElement(
                                          "p",
                                          {"className":"signup-message","style":{
                                                            color: isIdOk ? "#2e9e4f" : "#e5484d",
                                                          }},
                                          (getIdMessage())
                                        )
                            ),
                    React.createElement(
                              "div",
                              {"className":"signup-field"},
                              React.createElement(
                                          "label",
                                          {"htmlFor":"signupEmail"},
                                          "이메일"
                                        ),
                              React.createElement(
                                          "input",
                                          {"id":"signupEmail","type":"email","name":"email","value":inputEmail,"placeholder":"이메일을 입력해 주세요.","onChange":(e) => setInputEmail(e.target.value.trim()),"required":true}
                                        ),
                              React.createElement(
                                          "p",
                                          {"className":"signup-message","style":{
                                                            color: isEmail ? "#2e9e4f" : "#e5484d",
                                                          }},
                                          (inputEmail &&
                                                            (isEmail
                                                              ? "올바른 이메일 형식입니다."
                                                              : "이메일 형식이 올바르지 않습니다."))
                                        )
                            ),
                    React.createElement(
                              "div",
                              {"className":"signup-field"},
                              React.createElement(
                                          "label",
                                          {"htmlFor":"signupPassword"},
                                          "비밀번호"
                                        ),
                              React.createElement(
                                          "input",
                                          {"id":"signupPassword","type":"password","name":"password","value":inputPwd,"placeholder":"비밀번호를 입력해 주세요.","onChange":(e) => setInputPwd(e.target.value),"required":true}
                                        ),
                              React.createElement(
                                          "p",
                                          {"className":"signup-message","style":{
                                                            color: isPwd ? "#2e9e4f" : "#e5484d",
                                                          }},
                                          (inputPwd &&
                                                            (isPwd
                                                              ? "사용 가능한 비밀번호입니다."
                                                              : "영문, 숫자, 특수문자를 포함해 8~20자로 입력하세요."))
                                        )
                            ),
                    React.createElement(
                              "div",
                              {"className":"signup-field"},
                              React.createElement(
                                          "label",
                                          {"htmlFor":"signupPasswordCheck"},
                                          "비밀번호 확인"
                                        ),
                              React.createElement(
                                          "input",
                                          {"id":"signupPasswordCheck","type":"password","name":"passwordCheck","value":inputPwdCheck,"placeholder":"비밀번호를 다시 입력해 주세요.","onChange":(e) => setInputPwdCheck(e.target.value),"required":true}
                                        ),
                              React.createElement(
                                          "p",
                                          {"className":"signup-message","style":{
                                                            color: isPwdCheck ? "#2e9e4f" : "#e5484d",
                                                          }},
                                          (inputPwdCheck &&
                                                            (isPwdCheck
                                                              ? "비밀번호가 일치합니다."
                                                              : "비밀번호가 일치하지 않습니다."))
                                        )
                            ),
                    React.createElement(
                              "div",
                              {"className":"signup-field"},
                              React.createElement(
                                          "label",
                                          {"htmlFor":"signupNickname"},
                                          "닉네임"
                                        ),
                              React.createElement(
                                          "input",
                                          {"id":"signupNickname","type":"text","name":"nickname","value":inputNickname,"placeholder":"닉네임을 입력해 주세요.","onChange":(e) => setInputNickname(e.target.value.trim()),"required":true}
                                        ),
                              React.createElement(
                                          "p",
                                          {"className":"signup-message","style":{
                                                            color: isNickname ? "#2e9e4f" : "#e5484d",
                                                          }},
                                          (inputNickname &&
                                                            (isNickname
                                                              ? "사용 가능한 닉네임입니다."
                                                              : "한글, 영문, 숫자로 2~10자 입력하세요."))
                                        )
                            ),
                    React.createElement(
                              "button",
                              {"type":"submit","className":"signup-submit","disabled":!isAllValid},
                              "무료 회원가입하고 영화 메모하러 가기"
                            )
                  ),
            React.createElement(
                    "div",
                    {"className":"signup-footer"},
                    React.createElement(
                              "span",
                              null,
                              "이미 계정이 있으신가요?"
                            ),
                    React.createElement(
                              Link,
                              {"to":"/login"},
                              "로그인"
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

export default SignUp;
