package com.momo.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PosterFileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void detectsAnExistingLocalPosterFile() throws Exception {
        PosterFileStorageService storageService =
                new PosterFileStorageService(temporaryDirectory.toString());
        Files.writeString(temporaryDirectory.resolve("poster.jpg"), "image");

        assertTrue(storageService.exists("/posters/poster.jpg"));
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
