import React from "react";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import AxiosApi from "../../api/AxiosApi";
import ChatPopup from "./ChatPopup";

const defaultViewport = { width: window.innerWidth, height: window.innerHeight };

const setViewport = (width, height) => {
  Object.defineProperty(window, "innerWidth", { configurable: true, value: width });
  Object.defineProperty(window, "innerHeight", { configurable: true, value: height });
};

jest.mock("../../api/AxiosApi", () => ({
  __esModule: true,
  default: {
    memberMe: jest.fn(),
    chatImageUpload: jest.fn(),
    chatImageGet: jest.fn(),
    profileImageByUserIdGet: jest.fn(),
  },
}));

class MockWebSocket {
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;
  static instances = [];

  constructor(url) {
    this.url = url;
    this.readyState = 0;
    this.send = jest.fn();
    this.close = jest.fn((code = 1000) => {
      this.readyState = MockWebSocket.CLOSED;
      this.onclose?.({ code, wasClean: code === 1000 });
    });
    MockWebSocket.instances.push(this);
    Promise.resolve().then(() => {
      this.readyState = MockWebSocket.OPEN;
      this.onopen?.();
    });
  }

  receive(frame) {
    this.onmessage?.({ data: JSON.stringify(frame) });
  }

  drop() {
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.({ code: 1006, wasClean: false });
  }
}

describe("ChatPopup", () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem("accessToken", "test-access-token");
    AxiosApi.memberMe.mockResolvedValue({ data: { userId: 7 } });
    AxiosApi.chatImageUpload.mockResolvedValue({ data: { imageId: "9c9bb1a8-f922-4ea1-8c41-2f2d987eadd1" } });
    AxiosApi.chatImageGet.mockResolvedValue({ data: new Blob(["image-bytes"], { type: "image/png" }) });
    AxiosApi.profileImageByUserIdGet.mockResolvedValue({ data: new Blob(["avatar-bytes"], { type: "image/png" }) });
    AxiosApi.profileImageByUserIdGet.mockResolvedValue({ data: new Blob(["avatar"], { type: "image/png" }) });
    URL.createObjectURL = jest.fn(() => "blob:test-image");
    URL.revokeObjectURL = jest.fn();
    MockWebSocket.instances = [];
    global.WebSocket = MockWebSocket;
  });

  afterEach(() => {
    setViewport(defaultViewport.width, defaultViewport.height);
    jest.useRealTimers();
  });

  test("authenticates with Access Token, sends Unicode messages, and displays server notices", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));

    const socket = MockWebSocket.instances[0];
    await waitFor(() => expect(socket.send).toHaveBeenCalledWith(
      JSON.stringify({ type: "AUTH", accessToken: "test-access-token" }),
    ));

    act(() => {
      socket.receive({ type: "AUTHENTICATED", userId: 7, streamId: "stream-1" });
      socket.receive({ type: "PRESENCE", onlineCount: 3 });
      socket.receive({
        type: "HISTORY",
        messages: [{
          id: 1,
          userId: 7,
          nickname: "모모",
          profileImageAvailable: false,
          content: "안녕하세요 🎬",
          createdAt: new Date().toISOString(),
        }],
        hasMore: false,
      });
    });
    expect(await screen.findByText("안녕하세요 🎬")).toBeInTheDocument();
    expect(screen.getByText("접속 3명")).toBeInTheDocument();

    fireEvent.change(screen.getByRole("textbox", { name: "채팅 메시지" }), {
      target: { value: "😊 반가워요" },
    });
    fireEvent.click(screen.getByRole("button", { name: "전송" }));
    expect(socket.send).toHaveBeenLastCalledWith(
      JSON.stringify({ type: "SEND", content: "😊 반가워요" }),
    );

    act(() => socket.receive({ type: "ERROR", code: "RATE_LIMITED", detail: "최근 3초 동안 최대 3개 메시지만 보낼 수 있습니다." }));
    expect(await screen.findByRole("alert")).toHaveTextContent("최근 3초 동안 최대 3개");

    act(() => socket.receive({ type: "MESSAGE_DELETED", messageId: 1, detail: "작성자가 메시지를 삭제했습니다." }));
    expect(await screen.findByText("작성자가 메시지를 삭제했습니다.")).toBeInTheDocument();
  });

  test("keeps the socket available for unread alerts while the popup is hidden", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    const socket = MockWebSocket.instances[0];
    await waitFor(() => expect(socket.send).toHaveBeenCalled());
    act(() => socket.receive({ type: "AUTHENTICATED", userId: 7, streamId: "stream-1" }));

    fireEvent.click(screen.getAllByRole("button", { name: "채팅 닫기" })[0]);
    expect(socket.close).not.toHaveBeenCalled();
    act(() => socket.receive({
      type: "MESSAGE",
      message: { id: 5, userId: 9, nickname: "다른 회원", content: "새 소식", createdAt: new Date().toISOString() },
    }));
    expect(screen.getByLabelText("새 메시지 1개")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    expect(screen.queryByLabelText("새 메시지 1개")).not.toBeInTheDocument();
  });

  test("does not notify for the current member's own message", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    const socket = MockWebSocket.instances[0];
    act(() => socket.receive({ type: "AUTHENTICATED", userId: 7, streamId: "stream-1" }));

    act(() => socket.receive({
      type: "MESSAGE",
      message: { id: 8, userId: 7, nickname: "모모", content: "내 메시지", createdAt: new Date().toISOString() },
    }));

    expect(screen.queryByLabelText("새 메시지 1개")).not.toBeInTheDocument();
  });

  test("does not show a minimize control and keeps the message composer visible", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));

    expect(screen.queryByRole("button", { name: "채팅 최소화" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "채팅 복원" })).not.toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "채팅 메시지" })).toBeInTheDocument();
  });

  test("loads member avatars through the authenticated profile image API", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    act(() => MockWebSocket.instances[0].receive({ type: "AUTHENTICATED", userId: 7, streamId: "stream-1" }));
    act(() => MockWebSocket.instances[0].receive({
      type: "HISTORY",
      messages: [{
        id: 2,
        userId: 9,
        nickname: "회원",
        profileImageAvailable: true,
        content: "프로필 확인",
        createdAt: new Date().toISOString(),
      }],
      hasMore: false,
    }));

    await waitFor(() => expect(AxiosApi.profileImageByUserIdGet).toHaveBeenCalledWith(9, expect.any(AbortSignal)));
    expect(await screen.findByAltText("회원 프로필")).toHaveAttribute("src", "blob:test-image");
  });

  test("closes the active socket when local logout clears authentication", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    await waitFor(() => expect(MockWebSocket.instances[0].send).toHaveBeenCalled());

    localStorage.removeItem("accessToken");
    act(() => window.dispatchEvent(new Event("momo-auth-changed")));

    await waitFor(() => expect(MockWebSocket.instances[0].close).toHaveBeenCalled());
  });

  test("uploads the selected image over HTTP and sends only its ID over WebSocket", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    const socket = MockWebSocket.instances[0];
    act(() => socket.receive({ type: "AUTHENTICATED", userId: 7, streamId: "stream-1" }));

    const png = new File(["png"], "photo.png", { type: "image/png" });
    fireEvent.change(screen.getByLabelText("채팅 이미지 선택"), {
      target: { files: [png] },
    });

    await waitFor(() => expect(AxiosApi.chatImageUpload).toHaveBeenCalledWith(png));
    await waitFor(() => expect(socket.send).toHaveBeenCalledWith(JSON.stringify({
      type: "IMAGE",
      imageId: "9c9bb1a8-f922-4ea1-8c41-2f2d987eadd1",
    })));
    act(() => socket.receive({ type: "MESSAGE", message: {
      id: 21,
      userId: 7,
      nickname: "모모",
      messageType: "IMAGE",
      imageId: "9c9bb1a8-f922-4ea1-8c41-2f2d987eadd1",
      createdAt: new Date().toISOString(),
    } }));
    expect(await screen.findByText("이미지가 채팅에 전송되었습니다.")).toBeInTheDocument();
  });

  test("renders authenticated image messages and opens an enlarged preview", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    const socket = MockWebSocket.instances[0];
    act(() => {
      socket.receive({ type: "AUTHENTICATED", userId: 7, streamId: "stream-1" });
      socket.receive({ type: "MESSAGE", message: {
        id: 4,
        userId: 8,
        nickname: "회원",
        messageType: "IMAGE",
        imageId: "9c9bb1a8-f922-4ea1-8c41-2f2d987eadd1",
        createdAt: new Date().toISOString(),
      } });
    });

    expect(await screen.findByRole("img", { name: "채팅 이미지" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "이미지 크게 보기" }));
    expect(screen.getByRole("img", { name: "확대된 채팅 이미지" })).toHaveAttribute("src", "blob:test-image");
    expect(AxiosApi.chatImageGet).toHaveBeenCalledWith("9c9bb1a8-f922-4ea1-8c41-2f2d987eadd1", expect.any(AbortSignal));
  });

  test("reconnects with capped backoff, reauthenticates, and synchronizes missed IDs without duplicates", async () => {
    jest.useFakeTimers();
    render(<ChatPopup />);
    await act(async () => { await Promise.resolve(); await Promise.resolve(); });
    expect(MockWebSocket.instances).toHaveLength(1);

    const firstSocket = MockWebSocket.instances[0];
    await act(async () => { await Promise.resolve(); });
    act(() => {
      firstSocket.receive({ type: "AUTHENTICATED", userId: 7, streamId: "stream-1" });
      firstSocket.receive({
        type: "HISTORY",
        messages: [{ id: 1, userId: 8, nickname: "회원", content: "기존", createdAt: new Date().toISOString() }],
        hasMore: false,
      });
      firstSocket.drop();
    });
    expect(screen.getByText("재연결 중…")).toBeInTheDocument();

    await act(async () => {
      jest.advanceTimersByTime(499);
      await Promise.resolve();
    });
    expect(MockWebSocket.instances).toHaveLength(1);

    await act(async () => {
      jest.advanceTimersByTime(1);
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(MockWebSocket.instances).toHaveLength(2);
    const reconnectedSocket = MockWebSocket.instances[1];
    await act(async () => { await Promise.resolve(); });
    expect(reconnectedSocket.send).toHaveBeenCalledWith(
      JSON.stringify({ type: "AUTH", accessToken: "test-access-token" }),
    );

    act(() => reconnectedSocket.receive({ type: "AUTHENTICATED", userId: 7, streamId: "stream-1" }));
    expect(reconnectedSocket.send).toHaveBeenLastCalledWith(
      JSON.stringify({ type: "SYNC", afterId: 1, limit: 50 }),
    );

    act(() => {
      reconnectedSocket.receive({
        type: "SYNC",
        messages: [{ id: 2, userId: 9, nickname: "회원2", content: "누락 1", createdAt: new Date().toISOString() }],
        hasMore: true,
        nextAfterId: 2,
      });
    });
    expect(reconnectedSocket.send).toHaveBeenLastCalledWith(
      JSON.stringify({ type: "SYNC", afterId: 2, limit: 50 }),
    );
    act(() => {
      reconnectedSocket.receive({
        type: "SYNC",
        messages: [
          { id: 2, userId: 9, nickname: "회원2", content: "누락 1", createdAt: new Date().toISOString() },
          {
            id: 3,
            userId: 10,
            nickname: "회원3",
            messageType: "IMAGE",
            imageId: "9c9bb1a8-f922-4ea1-8c41-2f2d987eadd1",
            createdAt: new Date().toISOString(),
          },
        ],
        hasMore: false,
        nextAfterId: 3,
      });
    });

    expect(screen.getByLabelText("새 메시지 2개")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    expect(await screen.findByText("누락 1")).toBeInTheDocument();
    expect(await screen.findByRole("img", { name: "채팅 이미지" })).toBeInTheDocument();
    expect(screen.getAllByText("누락 1")).toHaveLength(1);
    expect(screen.queryByLabelText("새 메시지 2개")).not.toBeInTheDocument();
  });

  test("does not reconnect after JWT authentication failure or logout during backoff", async () => {
    jest.useFakeTimers();
    const { unmount } = render(<ChatPopup />);
    await act(async () => { await Promise.resolve(); await Promise.resolve(); });
    const firstSocket = MockWebSocket.instances[0];
    await act(async () => { await Promise.resolve(); });
    act(() => firstSocket.receive({ type: "ERROR", code: "INVALID_ACCESS_TOKEN", detail: "인증 실패" }));
    expect(firstSocket.close).toHaveBeenCalledWith(1000);
    await act(async () => { jest.advanceTimersByTime(60_000); });
    expect(MockWebSocket.instances).toHaveLength(1);

    unmount();
    MockWebSocket.instances = [];
    localStorage.setItem("accessToken", "test-access-token");
    render(<ChatPopup />);
    await act(async () => { await Promise.resolve(); await Promise.resolve(); });
    const logoutSocket = MockWebSocket.instances[0];
    await act(async () => { await Promise.resolve(); });
    act(() => logoutSocket.drop());
    localStorage.removeItem("accessToken");
    act(() => window.dispatchEvent(new Event("momo-auth-changed")));
    await act(async () => { jest.advanceTimersByTime(60_000); });
    expect(MockWebSocket.instances).toHaveLength(1);
  });

  test("supports bounded dragging and resizing on desktop", async () => {
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    const panel = screen.getByRole("region", { name: "전체 회원 채팅" });
    panel.getBoundingClientRect = () => ({ left: 500, top: 200, width: 380, height: 560, right: 880, bottom: 760 });

    fireEvent.mouseDown(panel.querySelector("header"), { button: 0, clientX: 510, clientY: 210 });
    fireEvent.mouseMove(window, { clientX: 6000, clientY: 6000 });
    fireEvent.mouseUp(window);
    expect(panel.style.left).toBe(`${window.innerWidth - 388}px`);
    expect(panel.style.top).toBe(`${window.innerHeight - 568}px`);

    panel.getBoundingClientRect = () => ({ left: 100, top: 100, width: 380, height: 560, right: 480, bottom: 660 });
    fireEvent.mouseDown(screen.getByRole("button", { name: "채팅창 크기 조절" }), {
      button: 0, clientX: 480, clientY: 660,
    });
    fireEvent.mouseMove(window, { clientX: 650, clientY: 750 });
    fireEvent.mouseUp(window);
    expect(Number.parseInt(panel.style.width, 10)).toBeGreaterThan(380);
    expect(Number.parseInt(panel.style.height, 10)).toBeGreaterThan(560);

    fireEvent.click(screen.getAllByRole("button", { name: "채팅 닫기" })[0]);
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    const reopenedPanel = screen.getByRole("region", { name: "전체 회원 채팅" });
    expect(reopenedPanel.style.left).toBe("");
    expect(reopenedPanel.style.top).toBe("");
    expect(reopenedPanel.style.width).toBe("380px");
    expect(reopenedPanel.style.height).toBe("560px");
  });

  test("fits a small browser window while keeping controls and message input accessible", async () => {
    setViewport(360, 420);
    render(<ChatPopup />);
    await waitFor(() => expect(MockWebSocket.instances).toHaveLength(1));
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    const panel = screen.getByRole("region", { name: "전체 회원 채팅" });

    expect(panel.style.width).toBe("328px");
    expect(panel.style.height).toBe("324px");
    expect(screen.getAllByRole("button", { name: "채팅 닫기" })).toHaveLength(2);
    expect(screen.getByRole("textbox", { name: "채팅 메시지" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "전송" })).toBeInTheDocument();

    setViewport(320, 300);
    act(() => fireEvent(window, new Event("resize")));
    expect(panel.style.width).toBe("288px");
    expect(panel.style.height).toBe("204px");
    expect(screen.getByRole("textbox", { name: "채팅 메시지" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "전송" })).toBeInTheDocument();
  });

  test("preserves a manually resized panel when possible and clamps it after the viewport shrinks", async () => {
    setViewport(1400, 900);
    render(<ChatPopup />);
    fireEvent.click(screen.getByRole("button", { name: "공용 채팅 열기" }));
    const panel = screen.getByRole("region", { name: "전체 회원 채팅" });
    panel.getBoundingClientRect = () => ({ left: 100, top: 100, width: 380, height: 560, right: 480, bottom: 660 });

    fireEvent.mouseDown(screen.getByRole("button", { name: "채팅창 크기 조절" }), {
      button: 0, clientX: 480, clientY: 660,
    });
    fireEvent.mouseMove(window, { clientX: 580, clientY: 660 });
    fireEvent.mouseUp(window);
    expect(panel.style.width).toBe("480px");
    expect(panel.style.height).toBe("560px");

    setViewport(1600, 1000);
    act(() => fireEvent(window, new Event("resize")));
    expect(panel.style.width).toBe("480px");
    expect(panel.style.height).toBe("560px");

    setViewport(500, 400);
    act(() => fireEvent(window, new Event("resize")));
    expect(panel.style.width).toBe("468px");
    expect(panel.style.height).toBe("304px");
    expect(Number.parseInt(panel.style.left, 10) + Number.parseInt(panel.style.width, 10))
      .toBeLessThanOrEqual(492);
    expect(Number.parseInt(panel.style.top, 10) + Number.parseInt(panel.style.height, 10))
      .toBeLessThanOrEqual(392);
    expect(screen.getByRole("textbox", { name: "채팅 메시지" })).toBeInTheDocument();
  });
});

