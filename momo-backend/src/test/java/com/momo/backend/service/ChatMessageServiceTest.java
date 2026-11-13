package com.momo.backend.service;

import com.momo.backend.dto.chat.ChatHistoryPage;
import com.momo.backend.dto.chat.ChatMessageResponse;
import com.momo.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatMessageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void keepsOnlyLatestThousandAndPagesOlderMessages() {
        ChatMessageService service = new ChatMessageService(new ChatProfanityFilter(new ChatBannedWords()));

        for (long index = 1; index <= 1_005; index++) {
            service.send(user(index), "message " + index);
        }

        ChatHistoryPage latest = service.getHistory(null, null);
        assertEquals(50, latest.messages().size());
        assertEquals(956L, latest.messages().getFirst().id());
        assertEquals(1_005L, latest.messages().getLast().id());
        assertTrue(latest.hasMore());

        ChatHistoryPage earlier = service.getHistory(latest.nextBeforeId(), 50);
        assertEquals(906L, earlier.messages().getFirst().id());
        assertEquals(955L, earlier.messages().getLast().id());
        assertTrue(earlier.hasMore());
    }

    @Test
    void returnsMessagesAfterReconnectCursorInBoundedPages() {
        ChatMessageService service = new ChatMessageService(new ChatProfanityFilter(new ChatBannedWords()));
        for (long index = 1; index <= 120; index++) {
            service.send(user(index), "message " + index);
        }

        ChatHistoryPage first = service.getMessagesAfter(10L, 50);
        assertEquals(50, first.messages().size());
        assertEquals(11L, first.messages().getFirst().id());
        assertEquals(60L, first.messages().getLast().id());
        assertTrue(first.hasMore());
        assertEquals(60L, first.nextAfterId());

        ChatHistoryPage second = service.getMessagesAfter(first.nextAfterId(), 50);
        assertEquals(50, second.messages().size());
        assertEquals(61L, second.messages().getFirst().id());
        assertEquals(110L, second.messages().getLast().id());
        assertTrue(second.hasMore());

        ChatHistoryPage last = service.getMessagesAfter(second.nextAfterId(), 50);
        assertEquals(List.of(111L, 112L, 113L, 114L, 115L, 116L, 117L, 118L, 119L, 120L),
                last.messages().stream().map(message -> message.id()).toList());
        assertFalse(last.hasMore());
    }

    @Test
    void rejectsFourthMessageWithinThreeSecondsWithoutStoringIt() {
        ChatMessageService service = new ChatMessageService(new ChatProfanityFilter(new ChatBannedWords()));
        User user = user(10L);

        service.send(user, "one");
        service.send(user, "two");
        service.send(user, "three");
        ChatRuleViolationException exception = assertThrows(
                ChatRuleViolationException.class,
                () -> service.send(user, "four"));

        assertEquals("RATE_LIMITED", exception.getCode());
        assertEquals(3, service.getHistory(null, 50).messages().size());
    }

    @Test
    void rejectsBannedAndOverlongMessages() {
        ChatMessageService profanityService = new ChatMessageService(new ChatProfanityFilter(new ChatBannedWords()));
        ChatRuleViolationException profanity = assertThrows(
                ChatRuleViolationException.class,
                () -> profanityService.send(user(1L), "씨발"));
        assertEquals("BANNED_WORD", profanity.getCode());
        assertTrue(profanityService.getHistory(null, 50).messages().isEmpty());

        ChatMessageService lengthService = new ChatMessageService(new ChatProfanityFilter(new ChatBannedWords()));
        ChatRuleViolationException tooLong = assertThrows(
                ChatRuleViolationException.class,
                () -> lengthService.send(user(2L), "😀".repeat(501)));
        assertEquals("MESSAGE_TOO_LONG", tooLong.getCode());
        assertTrue(lengthService.getHistory(null, 50).messages().isEmpty());
    }

    @Test
    void allowsOnlyAuthorToDeleteStoredMessage() {
        ChatMessageService service = new ChatMessageService(new ChatProfanityFilter(new ChatBannedWords()));
        User author = user(1L);
        User anotherMember = user(2L);
        ChatMessageResponse message = service.send(author, "hello");

        ChatRuleViolationException exception = assertThrows(
                ChatRuleViolationException.class,
                () -> service.deleteOwnMessage(anotherMember, message.id()));
        assertEquals("NOT_MESSAGE_OWNER", exception.getCode());
        assertEquals(1, service.getHistory(null, 50).messages().size());

        service.deleteOwnMessage(author, message.id());
        assertFalse(service.getHistory(null, 50).messages().stream()
                .anyMatch(item -> item.id() == message.id()));
    }

    @Test
    void imageMessagesUseTheSameHistoryAndDeleteLifecycle() throws Exception {
        ChatImageStorageService storage = new ChatImageStorageService(tempDirectory.toString());
        ChatMessageService service = new ChatMessageService(
                new ChatProfanityFilter(new ChatBannedWords()), storage);
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", imageBytes);
        User author = user(42L);
        String imageId = storage.store(new MockMultipartFile("file", "photo.png", "image/png", imageBytes.toByteArray()), 42L);

        ChatMessageResponse message = service.sendImage(author, imageId);
        assertEquals("IMAGE", message.messageType());
        assertTrue(service.containsImage(imageId));
        assertEquals(message.id(), service.getHistory(null, null).messages().getFirst().id());

        service.deleteOwnMessage(author, message.id());
        assertFalse(service.containsImage(imageId));
        assertTrue(service.getHistory(null, null).messages().isEmpty());
    }

    private User user(long id) {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(id);
        when(user.getLoginId()).thenReturn("member" + id);
        when(user.getNickname()).thenReturn("회원" + id);
        when(user.getProfileImage()).thenReturn(null);
        return user;
    }
}
