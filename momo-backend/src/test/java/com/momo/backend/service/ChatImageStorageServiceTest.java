package com.momo.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatImageStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void validatesActualPngDataAndAllowsOnlyUploaderToAttachIt() throws Exception {
        ChatImageStorageService service = new ChatImageStorageService(tempDirectory.toString());
        String imageId = service.store(pngFile("renamed.exe"), 7L);

        assertThrows(ChatRuleViolationException.class, () -> service.attachToMessage(imageId, 8L));
        assertThrows(ResponseStatusException.class, () -> service.read(imageId));

        service.attachToMessage(imageId, 7L);
        ChatImageStorageService.ImageContent image = service.read(imageId);
        assertEquals("image/png", image.contentType());
        assertTrue(image.bytes().length > 0);
        assertThrows(ChatRuleViolationException.class, () -> service.attachToMessage(imageId, 7L));

        service.delete(imageId);
        assertThrows(ResponseStatusException.class, () -> service.read(imageId));
    }

    @Test
    void rejectsSvgSpoofedImageAndFilesOverFiveMegabytes() throws Exception {
        ChatImageStorageService service = new ChatImageStorageService(tempDirectory.toString());
        MockMultipartFile svg = new MockMultipartFile("file", "vector.png", "image/png",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes());
        assertThrows(ResponseStatusException.class, () -> service.store(svg, 1L));

        MockMultipartFile fakePng = new MockMultipartFile("file", "fake.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1});
        assertThrows(ResponseStatusException.class, () -> service.store(fakePng, 1L));

        MockMultipartFile oversized = new MockMultipartFile("file", "large.png", "image/png",
                new byte[(int) ChatImageStorageService.MAX_IMAGE_BYTES + 1]);
        assertThrows(ResponseStatusException.class, () -> service.store(oversized, 1L));
    }

    @Test
    void clearsOrphanFilesBeforeTheNextUploadButKeepsLiveMessageFiles() throws Exception {
        ChatImageStorageService service = new ChatImageStorageService(tempDirectory.toString());
        String oldId = service.store(pngFile("old.png"), 1L);
        service.attachToMessage(oldId, 1L);
        Path oldFile;
        try (var files = Files.list(tempDirectory)) {
            oldFile = files.findFirst().orElseThrow();
        }

        // 새 인스턴스로 서버 재시작 후 메모리 이력이 없는 상황을 확인한다.
        ChatImageStorageService restarted = new ChatImageStorageService(tempDirectory.toString());
        restarted.store(pngFile("new.png"), 2L);

        assertFalse(Files.exists(oldFile));
    }

    private MockMultipartFile pngFile(String name) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("file", name, "image/png", bytes.toByteArray());
    }
}
