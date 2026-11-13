import styled, { createGlobalStyle } from "styled-components";

const ChatPopupGlobalStyles = createGlobalStyle``;
export default ChatPopupGlobalStyles;


export const ChatPanel = styled.section`
  position: fixed;
  right: 20px;
  bottom: 84px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
  min-width: 0;
  min-height: 0;
  border: 1px solid #d8d0c2;
  border-radius: 16px;
  background: #fffdf8;
  box-shadow: 0 12px 36px rgba(35, 28, 18, 0.22);

  @media (max-width: 600px) {
    max-width: calc(100vw - 32px);
  }
`;

export const ChatHeader = styled.header`
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  padding: 13px 14px;
  color: #382d21;
  background: #f2eadc;
  cursor: move;
  user-select: none;
`;

export const ChatHeading = styled.div`
  display: grid;
  gap: 3px;
`;

export const ConnectionStatus = styled.span`
  color: #665b4e;
  font-size: 12px;
`;

export const OnlineCount = styled.span`
  color: #493a2b;
  font-size: 12px;
  font-weight: 700;
`;

export const WindowActions = styled.div`
  display: flex;
  gap: 6px;
`;

export const WindowActionButton = styled.button`
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 10px;
  color: #fff;
  background: #493a2b;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
`;

export const MessageList = styled.div`
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px;
`;

export const MessageRow = styled.article`
  display: flex;
  flex-direction: ${({ $isOwn }) => ($isOwn ? "row-reverse" : "row")};
  align-items: flex-end;
  gap: 8px;
  margin: 12px 0;
`;

export const Avatar = styled.img`
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
`;

export const AvatarFallback = styled.span`
  display: grid;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  color: #493a2b;
  background: #e4d8c7;
`;

export const MessageBody = styled.div`
  min-width: 0;
  max-width: 82%;
`;

export const MessageMeta = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: ${({ $isOwn }) => ($isOwn ? "flex-end" : "flex-start")};
  gap: 7px;
  margin: 0 2px 4px;
  color: #493a2b;
  font-size: 12px;

  time {
    color: #817568;
    font-size: 10px;
  }
`;

export const DeleteButton = styled.button`
  padding: 0;
  border: 0;
  color: #766b5e;
  background: transparent;
  cursor: pointer;
  font-size: 11px;
`;

export const MessageBubble = styled.p`
  margin: 0;
  padding: 9px 12px;
  overflow-wrap: anywhere;
  border-radius: ${({ $isOwn }) => ($isOwn ? "14px 14px 4px 14px" : "14px 14px 14px 4px")};
  color: ${({ $isOwn, $isDeleted }) => ($isDeleted ? "#887f75" : $isOwn ? "#fffdf8" : "#241f19")};
  background: ${({ $isOwn, $isDeleted }) => ($isDeleted ? "#eeeae4" : $isOwn ? "#66513b" : "#f0e8dc")};
  line-height: 1.45;
  white-space: pre-wrap;
  font-style: ${({ $isDeleted }) => ($isDeleted ? "italic" : "normal")};
`;

export const MessageImage = styled.img`
  display: block;
  max-width: min(260px, 100%);
  max-height: 240px;
  border-radius: 12px;
  object-fit: contain;
  background: #f0e8dc;
  cursor: zoom-in;
`;

export const ImagePreviewButton = styled.button`
  display: block;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 12px;
  background: transparent;
  cursor: zoom-in;
`;

export const ImageModalOverlay = styled.div`
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(20, 16, 12, 0.82);
  cursor: zoom-out;
`;

export const ImageModalContent = styled.img`
  max-width: min(92vw, 1200px);
  max-height: 90vh;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.35);
`;

export const ImageUploadButton = styled.button`
  flex: 0 0 auto;
  padding: 9px 10px;
  border: 1px solid #d8d0c2;
  border-radius: 10px;
  color: #493a2b;
  background: #f2eadc;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
`;

export const HiddenFileInput = styled.input`
  display: none;
`;

export const SecondaryButton = styled.button`
  width: 100%;
  margin-bottom: 10px;
  padding: 8px;
  border: 0;
  border-radius: 9px;
  color: #493a2b;
  background: #eee5d7;
  cursor: pointer;
`;

export const EmptyMessage = styled.p`
  margin: 36px 0;
  color: #766b5e;
  text-align: center;
`;

export const MessageForm = styled.form`
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  min-height: 58px;
  box-sizing: border-box;
  padding: 12px;
  border-top: 1px solid #e4dccf;
`;

export const MessageInput = styled.input`
  flex: 1;
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid #d8d0c2;
  border-radius: 10px;
`;

export const SendButton = styled.button`
  padding: 9px 12px;
  border: 0;
  border-radius: 10px;
  color: #fff;
  background: #493a2b;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
`;

export const ErrorNotice = styled.p`
  margin: 0;
  padding: 0 12px 12px;
  color: #a3342a;
  font-size: 13px;
`;

export const SuccessNotice = styled.p`
  margin: 0;
  padding: 0 12px 12px;
  color: #287348;
  font-size: 13px;
`;

export const ResizeHandle = styled.button`
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 18px;
  height: 18px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: nwse-resize;

  &::after {
    position: absolute;
    right: 3px;
    bottom: 3px;
    width: 9px;
    height: 9px;
    border-right: 2px solid #a99d8d;
    border-bottom: 2px solid #a99d8d;
    content: "";
  }

  @media (max-width: 600px) {
    display: none;
  }
`;

export const ChatLauncher = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1001;
  padding: 13px 18px;
  border: 0;
  border-radius: 24px;
  color: #fff;
  background: #493a2b;
  cursor: pointer;
`;

export const UnreadBadge = styled.span`
  display: inline-grid;
  min-width: 20px;
  height: 20px;
  place-items: center;
  box-sizing: border-box;
  padding: 0 5px;
  border-radius: 10px;
  color: #493a2b;
  background: #f6c85f;
  font-size: 12px;
  font-weight: 700;
`;

export const ConnectionIndicator = styled.span`
  color: #f5dfbe;
  font-size: 11px;
  font-weight: 400;
`;

