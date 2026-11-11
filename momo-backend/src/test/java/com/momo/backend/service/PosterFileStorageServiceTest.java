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
        Files.writeString(temporaryDirectory.resolve("poster.jpg"), "image");

        assertTrue(storageService.exists("/posters/poster.jpg"));
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
