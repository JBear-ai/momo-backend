package com.momo.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PosterFileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsNumericAndAlphanumericKobisMovieCodes() {
        assertTrue(PosterFileStorageService.isValidMovieCode("20261109"));
        assertTrue(PosterFileStorageService.isValidMovieCode("2026A194"));
        assertFalse(PosterFileStorageService.isValidMovieCode("2026-A19"));
        assertFalse(PosterFileStorageService.isValidMovieCode("2026A194X"));
        assertFalse(PosterFileStorageService.isValidMovieCode(null));
    }

    @Test
    void detectsAnExistingLocalPosterFile() throws Exception {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());
        Files.write(
                temporaryDirectory.resolve("poster.jpg"),
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        assertTrue(storageService.exists("/posters/poster.jpg"));
    }

    @Test
    void recognizesAllImageFormatsSupportedByThePosterStorageService() throws Exception {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());

        Files.write(
                temporaryDirectory.resolve("poster.jpg"),
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );
        Files.write(
                temporaryDirectory.resolve("poster.png"),
                new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A
                }
        );
        Files.write(
                temporaryDirectory.resolve("poster.webp"),
                new byte[]{
                        'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00,
                        'W', 'E', 'B', 'P'
                }
        );

        assertTrue(storageService.exists("/posters/poster.jpg"));
        assertTrue(storageService.exists("/posters/poster.png"));
        assertTrue(storageService.exists("/posters/poster.webp"));
    }

    @Test
    void rejectsUntrustedPosterHostsBeforeOpeningANetworkConnection() {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());

        assertThrows(IllegalArgumentException.class,
                () -> storageService.downloadAndSave("20261109", "http://127.0.0.1/private"));
        assertThrows(IllegalArgumentException.class,
                () -> storageService.downloadAndSave("20261109", "https://example.com/poster.jpg"));
        assertThrows(IllegalArgumentException.class,
                () -> storageService.downloadAndSave(
                        "20261109", "https://user:password@image.tmdb.org/poster.jpg"));
    }

    @Test
    void rejectsFilesThatHaveAnImageExtensionButNoSupportedImageSignature() throws Exception {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());
        Files.writeString(temporaryDirectory.resolve("fake.jpg"), "<html>not an image</html>");

        assertFalse(storageService.exists("/posters/fake.jpg"));
    }

    @Test
    void rejectsPosterFilesLargerThanTheDownloadLimit() throws Exception {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());
        byte[] oversized = new byte[12 * 1024 * 1024 + 1];
        oversized[0] = (byte) 0xFF;
        oversized[1] = (byte) 0xD8;
        oversized[2] = (byte) 0xFF;
        Files.write(temporaryDirectory.resolve("oversized.jpg"), oversized);

        assertFalse(storageService.exists("/posters/oversized.jpg"));
    }

    @Test
    void rejectsSymbolicLinksInsteadOfFollowingFilesOutsidePosterDirectory() throws Exception {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());
        Path outside = temporaryDirectory.getParent().resolve("momo-outside-image.jpg");
        Files.write(outside, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        Path link = temporaryDirectory.resolve("linked.jpg");

        try {
            Files.createSymbolicLink(link, outside);
            assertFalse(storageService.exists("/posters/linked.jpg"));
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException ignored) {
            // Windows 실행 계정에 심볼릭 링크 권한이 없으면 해당 환경에서는 검증을 건너뛴다.
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void requestsOnlyImageFormatsSupportedByTheStorageValidator() {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());

        String acceptHeader = storageService.createDownloadRequest(
                URI.create("https://image.tmdb.org/t/p/w500/test.jpg"))
                .headers()
                .firstValue("Accept")
                .orElseThrow();

        assertEquals("image/webp,image/png,image/jpeg", acceptHeader);
        assertFalse(acceptHeader.contains("avif"));
    }

    @Test
    void deleteIgnoresPosterPathsThatEscapeOrTargetThePosterDirectory() throws Exception {
        Path posterDirectory = temporaryDirectory.resolve("posters");
        Files.createDirectories(posterDirectory);
        Path outsideFile = temporaryDirectory.resolve("outside.jpg");
        Files.writeString(outsideFile, "keep");

        PosterFileStorageService storageService =
                new PosterFileStorageService(posterDirectory.toString());

        storageService.delete("/posters/../outside.jpg");
        storageService.delete("/posters/.");
        storageService.delete("/posters/..");

        assertTrue(Files.exists(outsideFile));
        assertTrue(Files.isDirectory(posterDirectory));
    }

    @Test
    void reportsMissingOrUnsafePosterPathsAsUnavailable() {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());

        assertFalse(storageService.exists("/posters/missing.jpg"));
        assertFalse(storageService.exists("/posters/../outside.jpg"));
        assertFalse(storageService.exists("/posters/subfolder/poster.jpg"));
        assertFalse(storageService.exists("https://image.tmdb.org/poster.jpg"));
        assertFalse(storageService.exists(null));
    }
}
