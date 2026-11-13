import React, { useEffect, useRef, useState } from "react";
import AxiosApi from "../../api/AxiosApi";
import {
  Avatar,
  AvatarFallback,
  ChatHeader,
  ChatHeading,
  ChatLauncher,
  ChatPanel,
  ConnectionIndicator,
  ConnectionStatus,
  DeleteButton,
  EmptyMessage,
  ErrorNotice,
  HiddenFileInput,
  ImageModalContent,
  ImageModalOverlay,
  ImagePreviewButton,
  ImageUploadButton,
  MessageBody,
  MessageBubble,
  MessageForm,
  MessageInput,
  MessageImage,
  MessageList,
  MessageMeta,
  MessageRow,
  OnlineCount,
  ResizeHandle,
  SecondaryButton,
  SendButton,
  SuccessNotice,
  WindowActionButton,
  WindowActions,
  UnreadBadge,
} from "../../styles/legacy/chatPopup.style";

const HTTP_BASE_URL = (
  process.env.REACT_APP_API_BASE_URL || "http://localhost:8111"
).replace(/\/+$/, "");
const WS_BASE_URL = HTTP_BASE_URL.replace(/^http:/i, "ws:").replace(
  /^https:/i,
  "wss:",
);
const BASE_RECONNECT_DELAY_MS = 500;
const MAX_RECONNECT_DELAY_MS = 30_000;
const AUTH_FAILURE_CODES = new Set([
  "AUTH_REQUIRED",
  "INVALID_ACCESS_TOKEN",
  "ACCESS_TOKEN_EXPIRED",
  "MEMBER_NOT_FOUND",
]);

const mergeMessagesById = (current, incoming) => {
  const byId = new Map(current.map((message) => [message.id, message]));
  incoming.forEach((message) => byId.set(message.id, message));
  return [...byId.values()].sort((left, right) => left.id - right.id);
};

const reconnectDelay = (attempt) =>
  Math.min(
    BASE_RECONNECT_DELAY_MS * 2 ** Math.min(attempt, 16),
    MAX_RECONNECT_DELAY_MS,
  );

// 초기 크기와 창 크기 변경 시 기본 팝업이 뷰포트 안에 들어오도록 계산한다.
const getDefaultBounds = () => ({
  x: null,
  y: null,
  width: Math.max(1, Math.min(380, window.innerWidth - 32)),
  height: Math.max(1, Math.min(560, window.innerHeight - 96)),
});

// 인증된 이미지 API에서 이미지를 받아 미리보기로 표시한다.
function ChatImagePreview({ imageId, onSelect }) {
  const [source, setSource] = useState("");
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    let objectUrl = "";
    AxiosApi.chatImageGet(imageId, controller.signal)
      .then((response) => {
        objectUrl = URL.createObjectURL(response.data);
        setSource(objectUrl);
      })
      .catch((requestError) => {
        if (requestError.code !== "ERR_CANCELED") setFailed(true);
      });
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [imageId]);

  if (failed) return <span role="status">이미지를 불러오지 못했습니다.</span>;
  if (!source) return <span role="status">이미지 불러오는 중…</span>;
  return (
    <ImagePreviewButton
      type="button"
      onClick={() => onSelect(source)}
      aria-label="이미지 크게 보기"
    >
      <MessageImage src={source} alt="채팅 이미지" />
    </ImagePreviewButton>
  );
}

/** 로그인 회원이 이용하는 단일 공용 채팅 팝업과 브라우저 WebSocket 연결을 관리한다. */
export default function ChatPopup() {
  // 팝업 표시, 최소화, 크기/위치 상태를 분리해 창 크기 변경과 복원이 서로 덮어쓰지 않게 한다.
  const [open, setOpen] = useState(false);
  const [minimized, setMinimized] = useState(false);
  const [bounds, setBounds] = useState(getDefaultBounds);
  const [status, setStatus] = useState("closed");
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [error, setError] = useState("");
  const [hasMore, setHasMore] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [myUserId, setMyUserId] = useState(null);
  const [onlineCount, setOnlineCount] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [selectedImage, setSelectedImage] = useState("");
  const [uploadNotice, setUploadNotice] = useState("");
  const socketRef = useRef(null);
  const loadingOlderRef = useRef(false);
  const panelRef = useRef(null);
  const dragRef = useRef(null);
  const resizeRef = useRef(null);
  const userResizedRef = useRef(false);
  const openRef = useRef(false);
  const minimizedRef = useRef(false);
  const myUserIdRef = useRef(null);
  const shouldConnectRef = useRef(false);
  const connectingRef = useRef(false);
  const reconnectTimerRef = useRef(null);
  const reconnectAttemptsRef = useRef(0);
  const knownMessageIdsRef = useRef(new Set());
  const lastMessageIdRef = useRef(null);
  const streamIdRef = useRef(null);
  const syncCursorRef = useRef(null);
  const syncInProgressRef = useRef(false);
  const imageInputRef = useRef(null);

  const mergeIncomingMessages = (incoming, shouldNotify = false) => {
    const unseen = incoming.filter((message) => {
      if (knownMessageIdsRef.current.has(message.id)) return false;
      knownMessageIdsRef.current.add(message.id);
      lastMessageIdRef.current =
        lastMessageIdRef.current === null
          ? message.id
          : Math.max(lastMessageIdRef.current, message.id);
      return true;
    });
    if (unseen.length === 0) return;

    setMessages((current) => mergeMessagesById(current, unseen));
    if (shouldNotify && (!openRef.current || minimizedRef.current)) {
      const remoteMessages = unseen.filter(
        (message) => message.userId !== myUserIdRef.current,
      );
      if (remoteMessages.length > 0) {
        setUnreadCount((current) => current + remoteMessages.length);
      }
    }
  };

  // 이동과 크기 변경 뒤 팝업 전체가 화면 안에 남도록 크기와 좌표를 제한한다.
  const clampBounds = (next) => {
    const maxWidth = Math.max(1, window.innerWidth - 32);
    const maxHeight = Math.max(1, window.innerHeight - 96);
    const width = Math.min(
      Math.max(next.width, Math.min(300, maxWidth)),
      maxWidth,
    );
    const height = Math.min(
      Math.max(next.height, Math.min(240, maxHeight)),
      maxHeight,
    );
    return {
      x: Math.min(
        Math.max(next.x, 8),
        Math.max(8, window.innerWidth - width - 8),
      ),
      y: Math.min(
        Math.max(next.y, 8),
        Math.max(8, window.innerHeight - height - 8),
      ),
      width,
      height,
    };
  };

  const startDrag = (event) => {
    // 헤더의 창 제어 버튼은 드래그 대상으로 취급하지 않는다.
    if (event.button !== 0 || event.target.closest("button")) return;
    const rect = panelRef.current.getBoundingClientRect();
    dragRef.current = {
      x: event.clientX,
      y: event.clientY,
      left: rect.left,
      top: rect.top,
    };
    event.preventDefault();
  };

  const startResize = (event) => {
    // 사용자가 정한 크기는 이후 창이 커져도 유지하고, 작아질 때만 제한한다.
    userResizedRef.current = true;
    const rect = panelRef.current.getBoundingClientRect();
    resizeRef.current = {
      x: event.clientX,
      y: event.clientY,
      width: rect.width,
      height: rect.height,
    };
    event.preventDefault();
  };

  useEffect(() => {
    const move = (event) => {
      if (dragRef.current) {
        const drag = dragRef.current;
        setBounds((current) =>
          clampBounds({
            ...current,
            x: drag.left + event.clientX - drag.x,
            y: drag.top + event.clientY - drag.y,
          }),
        );
      } else if (resizeRef.current) {
        const resize = resizeRef.current;
        setBounds((current) =>
          clampBounds({
            ...current,
            width: resize.width + event.clientX - resize.x,
            height: resize.height + event.clientY - resize.y,
            x: panelRef.current?.getBoundingClientRect().left ?? current.x,
            y: panelRef.current?.getBoundingClientRect().top ?? current.y,
          }),
        );
      }
    };
    const stop = () => {
      dragRef.current = null;
      resizeRef.current = null;
    };
    // 직접 크기를 바꾼 팝업은 크기를 보존하고, 뷰포트를 넘을 때만 줄이거나 이동한다.
    const resizeWindow = () =>
      setBounds((current) => {
        const responsiveSize = userResizedRef.current
          ? current
          : {
              ...current,
              width: Math.min(380, Math.max(1, window.innerWidth - 32)),
              height: Math.min(560, Math.max(1, window.innerHeight - 96)),
            };
        const clamped = clampBounds({
          ...responsiveSize,
          x: current.x ?? 8,
          y: current.y ?? 8,
        });
        // 아직 드래그하지 않은 기본 팝업은 오른쪽 아래 기준 위치를 유지한다.
        return {
          ...clamped,
          x: current.x === null ? null : clamped.x,
          y: current.y === null ? null : clamped.y,
        };
      });
    window.addEventListener("mousemove", move);
    window.addEventListener("mouseup", stop);
    window.addEventListener("resize", resizeWindow);
    return () => {
      window.removeEventListener("mousemove", move);
      window.removeEventListener("mouseup", stop);
      window.removeEventListener("resize", resizeWindow);
    };
  }, []);

  useEffect(() => {
    shouldConnectRef.current = true;

    const clearReconnectTimer = () => {
      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
    };

    const stopForAuthenticationFailure = (message) => {
      shouldConnectRef.current = false;
      clearReconnectTimer();
      setStatus("closed");
      setError(message);
      const socket = socketRef.current;
      if (socket && socket.readyState < WebSocket.CLOSING) socket.close(1000);
    };

    let connect;
    const scheduleReconnect = () => {
      if (!shouldConnectRef.current || reconnectTimerRef.current !== null)
        return;
      const delay = reconnectDelay(reconnectAttemptsRef.current);
      reconnectAttemptsRef.current += 1;
      setStatus("reconnecting");
      setError("채팅 연결이 끊겨 다시 연결을 시도하고 있습니다.");
      reconnectTimerRef.current = window.setTimeout(() => {
        reconnectTimerRef.current = null;
        connect(true);
      }, delay);
    };

    connect = async (isReconnect = false) => {
      if (
        !shouldConnectRef.current ||
        connectingRef.current ||
        socketRef.current
      )
        return;
      connectingRef.current = true;
      setStatus(isReconnect ? "reconnecting" : "connecting");
      try {
        // 매 연결 전에 기존 HTTP 인증 경로를 통해 만료된 Access Token의 재발급을 허용한다.
        await AxiosApi.memberMe();
        if (!shouldConnectRef.current) return;

        const accessToken = localStorage.getItem("accessToken");
        if (!accessToken) {
          stopForAuthenticationFailure(
            "로그인이 만료되었습니다. 다시 로그인해 주세요.",
          );
          return;
        }

        const socket = new WebSocket(`${WS_BASE_URL}/ws/chat`);
        socketRef.current = socket;
        connectingRef.current = false;

        socket.onopen = () => {
          if (!shouldConnectRef.current) {
            socket.close(1000);
            return;
          }
          // JWT는 handshake URL에 넣지 않고 연결된 뒤 첫 데이터 프레임으로 인증한다.
          socket.send(JSON.stringify({ type: "AUTH", accessToken }));
        };

        socket.onmessage = (event) => {
          let frame;
          try {
            frame = JSON.parse(event.data);
          } catch {
            setError("채팅 서버 응답을 읽을 수 없습니다.");
            return;
          }

          switch (frame.type) {
            case "AUTHENTICATED": {
              myUserIdRef.current = frame.userId;
              setMyUserId(frame.userId);
              setStatus("connected");
              setError("");
              reconnectAttemptsRef.current = 0;

              const newStreamId = frame.streamId || "legacy-stream";
              const sameStream = streamIdRef.current === newStreamId;
              const resumeAfterId = lastMessageIdRef.current;
              if (streamIdRef.current !== null && !sameStream) {
                knownMessageIdsRef.current.clear();
                lastMessageIdRef.current = null;
                setMessages([]);
                setUnreadCount(0);
              }
              streamIdRef.current = newStreamId;

              if (sameStream && resumeAfterId !== null) {
                syncCursorRef.current = resumeAfterId;
                syncInProgressRef.current = true;
                socket.send(
                  JSON.stringify({
                    type: "SYNC",
                    afterId: resumeAfterId,
                    limit: 50,
                  }),
                );
              }
              break;
            }
            case "PRESENCE":
              setOnlineCount(Number(frame.onlineCount) || 0);
              break;
            case "HISTORY":
              mergeIncomingMessages(frame.messages || []);
              setHasMore(Boolean(frame.hasMore));
              loadingOlderRef.current = false;
              setLoadingOlder(false);
              break;
            case "SYNC": {
              mergeIncomingMessages(frame.messages || [], true);
              const nextCursor = frame.nextAfterId;
              if (
                frame.hasMore &&
                nextCursor !== null &&
                nextCursor !== undefined &&
                nextCursor !== syncCursorRef.current
              ) {
                syncCursorRef.current = nextCursor;
                socket.send(
                  JSON.stringify({
                    type: "SYNC",
                    afterId: nextCursor,
                    limit: 50,
                  }),
                );
              } else {
                syncInProgressRef.current = false;
                syncCursorRef.current = null;
              }
              break;
            }
            case "MESSAGE":
              if (frame.message) {
                if (
                  frame.message.messageType === "IMAGE" &&
                  frame.message.userId === myUserIdRef.current
                ) {
                  setUploadNotice("이미지가 채팅에 전송되었습니다.");
                }
                mergeIncomingMessages([frame.message], true);
              }
              break;
            case "MESSAGE_DELETED":
              setMessages((current) =>
                current.map((message) =>
                  message.id === frame.messageId
                    ? {
                        ...message,
                        deleted: true,
                        content:
                          frame.detail || "작성자가 메시지를 삭제했습니다.",
                      }
                    : message,
                ),
              );
              break;
            case "ERROR":
              setUploadNotice("");
              setError(frame.detail || "채팅 요청을 처리하지 못했습니다.");
              if (AUTH_FAILURE_CODES.has(frame.code)) {
                stopForAuthenticationFailure(
                  frame.detail ||
                    "로그인이 만료되었습니다. 다시 로그인해 주세요.",
                );
              } else if (
                frame.code === "UNKNOWN_REQUEST" &&
                syncInProgressRef.current
              ) {
                syncInProgressRef.current = false;
                syncCursorRef.current = null;
              }
              break;
            default:
              break;
          }
        };

        socket.onerror = () => setError("채팅 서버 연결이 불안정합니다.");
        socket.onclose = (event) => {
          if (socketRef.current === socket) socketRef.current = null;
          setOnlineCount(0);
          loadingOlderRef.current = false;
          setLoadingOlder(false);
          if (!shouldConnectRef.current) return;

          // 정책 위반 종료는 인증 실패로 취급하고, 정상 종료도 자동 재연결하지 않는다.
          if (event.code === 1008) {
            stopForAuthenticationFailure(
              "로그인이 만료되었거나 유효하지 않습니다. 다시 로그인해 주세요.",
            );
            return;
          }
          if (event.code === 1000) {
            setStatus("closed");
            return;
          }
          if (syncInProgressRef.current && syncCursorRef.current !== null) {
            lastMessageIdRef.current = syncCursorRef.current;
          }
          scheduleReconnect();
        };
      } catch (requestError) {
        if (!shouldConnectRef.current) return;
        connectingRef.current = false;
        const statusCode = requestError.response?.status;
        if (
          statusCode === 401 ||
          statusCode === 403 ||
          !localStorage.getItem("accessToken")
        ) {
          stopForAuthenticationFailure(
            "로그인이 만료되었습니다. 다시 로그인해 주세요.",
          );
        } else {
          scheduleReconnect();
        }
      } finally {
        connectingRef.current = false;
      }
    };

    connect();
    return () => {
      shouldConnectRef.current = false;
      clearReconnectTimer();
      const socket = socketRef.current;
      socketRef.current = null;
      // 컴포넌트 종료 시 재시도 예약과 소켓을 함께 정리한다.
      if (socket && socket.readyState < WebSocket.CLOSING) socket.close(1000);
      connectingRef.current = false;
      loadingOlderRef.current = false;
    };
  }, []);

  useEffect(() => {
    const closeOnLogout = () => {
      if (!localStorage.getItem("accessToken")) {
        shouldConnectRef.current = false;
        if (reconnectTimerRef.current !== null) {
          window.clearTimeout(reconnectTimerRef.current);
          reconnectTimerRef.current = null;
        }
        const socket = socketRef.current;
        socketRef.current = null;
        if (socket && socket.readyState < WebSocket.CLOSING) socket.close(1000);
        setOpen(false);
        openRef.current = false;
        setMinimized(false);
        minimizedRef.current = false;
        setMessages([]);
        knownMessageIdsRef.current.clear();
        lastMessageIdRef.current = null;
        streamIdRef.current = null;
        syncCursorRef.current = null;
        syncInProgressRef.current = false;
        setUnreadCount(0);
        setOnlineCount(0);
        setMyUserId(null);
        myUserIdRef.current = null;
        setStatus("closed");
      }
    };
    window.addEventListener("momo-auth-changed", closeOnLogout);
    return () => window.removeEventListener("momo-auth-changed", closeOnLogout);
  }, []);

  const sendFrame = (frame) => {
    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      setError("채팅 서버와 연결되어 있지 않습니다. 팝업을 다시 열어 주세요.");
      return false;
    }
    socket.send(JSON.stringify(frame));
    return true;
  };

  const togglePopup = () => {
    if (openRef.current) {
      openRef.current = false;
      setOpen(false);
      return;
    }
    openRef.current = true;
    minimizedRef.current = false;
    setMinimized(false);
    setUnreadCount(0);
    setOpen(true);
  };

  const toggleMinimized = () => {
    const nextMinimized = !minimizedRef.current;
    minimizedRef.current = nextMinimized;
    setMinimized(nextMinimized);
    if (!nextMinimized) setUnreadCount(0);
  };

  const sendMessage = (event) => {
    event.preventDefault();
    const content = input.trim();
    if (!content || status !== "connected") return;
    if (sendFrame({ type: "SEND", content })) {
      setInput("");
      setError("");
    }
  };

  const uploadAndSendImage = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
    if (!allowedTypes.includes(file.type)) {
      setError("JPEG, PNG, WebP 이미지만 전송할 수 있습니다.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("이미지는 최대 5MB까지 전송할 수 있습니다.");
      return;
    }
    if (status !== "connected") {
      setError("채팅 서버에 연결된 후 이미지를 전송해 주세요.");
      return;
    }

    setUploadingImage(true);
    setError("");
    setUploadNotice("");
    try {
      const response = await AxiosApi.chatImageUpload(file);
      if (!sendFrame({ type: "IMAGE", imageId: response.data.imageId })) {
        setError(
          "이미지 업로드는 완료됐지만 채팅 메시지 전송에 실패했습니다. 다시 시도해 주세요.",
        );
      } else {
        setUploadNotice("업로드 완료, 채팅 전송 확인 중…");
      }
    } catch (uploadError) {
      setError(
        uploadError.response?.data?.message ||
          "이미지를 업로드하지 못했습니다.",
      );
    } finally {
      setUploadingImage(false);
    }
  };

  const loadOlder = () => {
    if (loadingOlderRef.current || !messages.length) return;
    loadingOlderRef.current = true;
    setLoadingOlder(true);
    if (!sendFrame({ type: "HISTORY", beforeId: messages[0].id, limit: 50 })) {
      loadingOlderRef.current = false;
      setLoadingOlder(false);
    }
  };

  const deleteMessage = (messageId) => {
    setError("");
    sendFrame({ type: "DELETE", messageId });
  };

  return (
    <>
      {open && (
        <ChatPanel
          ref={panelRef}
          aria-label="전체 회원 채팅"
          style={
            bounds.x === null
              ? { width: bounds.width, height: minimized ? 58 : bounds.height }
              : {
                  left: bounds.x,
                  top: bounds.y,
                  width: bounds.width,
                  height: minimized ? 58 : bounds.height,
                }
          }
        >
          <ChatHeader onMouseDown={startDrag}>
            <ChatHeading>
              <strong>모모 라운지</strong>
              <ConnectionStatus>
                {status === "connected"
                  ? "연결됨"
                  : status === "connecting"
                    ? "연결 중…"
                    : status === "reconnecting"
                      ? "재연결 중…"
                      : "연결 종료"}
              </ConnectionStatus>
              <OnlineCount>접속 {onlineCount}명</OnlineCount>
              {minimized && unreadCount > 0 && (
                <UnreadBadge aria-label={`새 메시지 ${unreadCount}개`}>
                  {unreadCount > 99 ? "99+" : unreadCount}
                </UnreadBadge>
              )}
            </ChatHeading>
            <WindowActions>
              <WindowActionButton
                type="button"
                onClick={toggleMinimized}
                aria-label={minimized ? "채팅 복원" : "채팅 최소화"}
              >
                {minimized ? "□" : "−"}
              </WindowActionButton>
              <WindowActionButton
                type="button"
                onClick={togglePopup}
                aria-label="채팅 닫기"
              >
                ×
              </WindowActionButton>
            </WindowActions>
          </ChatHeader>

          {!minimized && (
            <>
              <MessageList aria-live="polite">
                {hasMore && (
                  <SecondaryButton
                    type="button"
                    onClick={loadOlder}
                    disabled={loadingOlder}
                  >
                    {loadingOlder
                      ? "이전 대화 불러오는 중…"
                      : "이전 대화 불러오기"}
                  </SecondaryButton>
                )}
                {messages.map((message) => (
                  <MessageRow
                    key={message.id}
                    $isOwn={myUserId === message.userId}
                  >
                    {message.profileImageAvailable && !message.deleted ? (
                      <Avatar
                        src={`${HTTP_BASE_URL}/api/auth/users/${message.userId}/profile-image`}
                        alt=""
                        width="32"
                        height="32"
                      />
                    ) : (
                      <AvatarFallback aria-hidden="true">
                        {message.nickname?.slice(0, 1) || "?"}
                      </AvatarFallback>
                    )}
                    <MessageBody>
                      <MessageMeta $isOwn={myUserId === message.userId}>
                        <strong>{message.nickname}</strong>
                        <time>
                          {new Date(message.createdAt).toLocaleTimeString([], {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </time>
                        {myUserId === message.userId && !message.deleted && (
                          <DeleteButton
                            type="button"
                            onClick={() => deleteMessage(message.id)}
                          >
                            삭제
                          </DeleteButton>
                        )}
                      </MessageMeta>
                      {message.messageType === "IMAGE" && !message.deleted ? (
                        <ChatImagePreview
                          imageId={message.imageId}
                          onSelect={setSelectedImage}
                        />
                      ) : (
                        <MessageBubble
                          $isOwn={myUserId === message.userId}
                          $isDeleted={message.deleted}
                        >
                          {message.content}
                        </MessageBubble>
                      )}
                    </MessageBody>
                  </MessageRow>
                ))}
                {messages.length === 0 && status === "connected" && (
                  <EmptyMessage>
                    아직 대화가 없습니다. 첫 메시지를 남겨 보세요.
                  </EmptyMessage>
                )}
              </MessageList>

              <MessageForm onSubmit={sendMessage}>
                <HiddenFileInput
                  ref={imageInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={uploadAndSendImage}
                  aria-label="채팅 이미지 선택"
                />
                <ImageUploadButton
                  type="button"
                  onClick={() => imageInputRef.current?.click()}
                  disabled={status !== "connected" || uploadingImage}
                >
                  {uploadingImage ? "업로드 중…" : "이미지"}
                </ImageUploadButton>
                <MessageInput
                  value={input}
                  onChange={(event) => {
                    setInput(event.target.value);
                    setError("");
                  }}
                  maxLength={500}
                  disabled={status !== "connected" || uploadingImage}
                  placeholder={
                    status === "connected"
                      ? "메시지를 입력하세요 (최대 500자)"
                      : "로그인 연결 중…"
                  }
                  aria-label="채팅 메시지"
                />
                <SendButton
                  type="submit"
                  disabled={
                    status !== "connected" || uploadingImage || !input.trim()
                  }
                >
                  전송
                </SendButton>
              </MessageForm>
              {error && <ErrorNotice role="alert">{error}</ErrorNotice>}
              {uploadNotice && (
                <SuccessNotice role="status">{uploadNotice}</SuccessNotice>
              )}
              <ResizeHandle
                type="button"
                aria-label="채팅창 크기 조절"
                onMouseDown={startResize}
              />
            </>
          )}
        </ChatPanel>
      )}

      {selectedImage && (
        <ImageModalOverlay
          role="presentation"
          onClick={() => setSelectedImage("")}
        >
          <ImageModalContent src={selectedImage} alt="확대된 채팅 이미지" />
        </ImageModalOverlay>
      )}

      <ChatLauncher
        type="button"
        onClick={togglePopup}
        aria-expanded={open}
        aria-label={open ? "채팅 닫기" : "공용 채팅 열기"}
      >
        <span>{"💬"}</span>
        {!open && unreadCount > 0 && (
          <UnreadBadge aria-label={`새 메시지 ${unreadCount}개`}>
            {unreadCount > 99 ? "99+" : unreadCount}
          </UnreadBadge>
        )}
        {!open && status !== "connected" && (
          <ConnectionIndicator>
            {status === "reconnecting"
              ? "재연결 중…"
              : status === "connecting"
                ? "연결 중…"
                : "오프라인"}
          </ConnectionIndicator>
        )}
      </ChatLauncher>
    </>
  );
}

