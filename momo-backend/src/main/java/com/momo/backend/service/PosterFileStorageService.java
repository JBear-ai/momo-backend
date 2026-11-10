package com.momo.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class PosterFileStorageService {

    private static final int MAX_IMAGE_BYTES = 12 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "file.koreafilm.or.kr",
            "image.tmdb.org"
    );

    private final Path posterDirectory;
    private final HttpClient httpClient;

    public PosterFileStorageService(
            @Value("${momo.storage.poster-dir:${user.home}/momo-data/posters}") String directory
    ) {
        this.posterDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public String downloadAndSave(String movieCode, String imageUrl) {
        if (movieCode == null || !movieCode.matches("\\d{8}")) {
            throw new IllegalArgumentException("올바르지 않은 영화 코드입니다.");
        }

        URI source = validateUri(imageUrl);
        DownloadedImage image = download(source, 0);
        String extension = extensionFor(image.contentType());
        String fileName = movieCode + "-" + UUID.randomUUID() + extension;
        Path destination = posterDirectory.resolve(fileName).normalize();

        if (!destination.startsWith(posterDirectory)) {
            throw new IllegalArgumentException("허용되지 않는 이미지 저장 경로입니다.");
        }

        try {
            Files.createDirectories(posterDirectory);
            Path temporary = Files.createTempFile(posterDirectory, movieCode + "-", ".tmp");
            try {
                Files.write(temporary, image.bytes());
                try {
                    Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("포스터 파일 저장에 실패했습니다.", exception);
        }

        return "/posters/" + fileName;
    }

    public void delete(String posterPath) {
        if (posterPath == null || !posterPath.startsWith("/posters/")) {
            return;
        }
        String fileName = posterPath.substring("/posters/".length());
        if (fileName.contains("/") || fileName.contains("\\")) {
            return;
        }
        try {
            Files.deleteIfExists(posterDirectory.resolve(fileName).normalize());
        } catch (IOException exception) {
            log.warn("사용되지 않는 포스터 파일 삭제 실패: {}", fileName, exception);
        }
    }

    public String location() {
        return posterDirectory.toUri().toString();
    }

    private DownloadedImage download(URI uri, int redirectCount) {
        validateUri(uri.toString());

        if (redirectCount > MAX_REDIRECTS) {
            throw new IllegalStateException("포스터 다운로드 리다이렉트 횟수를 초과했습니다.");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "image/avif,image/webp,image/png,image/jpeg")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new IllegalStateException("포스터 서버가 잘못된 리다이렉트 응답을 반환했습니다."));
                try (InputStream ignored = response.body()) {
                    return download(uri.resolve(location), redirectCount + 1);
                }
            }

            if (status != 200) {
                try (InputStream ignored = response.body()) {
                    throw new IllegalStateException("포스터 서버 응답 오류: HTTP " + status);
                }
            }

            String contentType = response.headers().firstValue("content-type")
                    .orElse("")
                    .split(";")[0]
                    .trim()
                    .toLowerCase(Locale.ROOT);

            long contentLength = response.headers().firstValueAsLong("content-length").orElse(-1);
            if (contentLength > MAX_IMAGE_BYTES || !isAllowedImageType(contentType)) {
                try (InputStream ignored = response.body()) {
                    throw new IllegalStateException("허용되지 않는 포스터 파일 형식 또는 크기입니다.");
                }
            }

            byte[] bytes;
            try (InputStream input = response.body();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                int total = 0;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_IMAGE_BYTES) {
                        throw new IllegalStateException("포스터 파일이 최대 크기 12MB를 초과했습니다.");
                    }
                    output.write(buffer, 0, read);
                }
                bytes = output.toByteArray();
            }

            validateImageSignature(bytes, contentType);
            return new DownloadedImage(bytes, contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("포스터 다운로드에 실패했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("포스터 다운로드가 중단되었습니다.", exception);
        }
    }

    private URI validateUri(String value) {
        if (value == null || value.isBlank() || value.length() > 2000) {
            throw new IllegalArgumentException("유효하지 않은 포스터 주소입니다.");
        }
        try {
            URI uri = URI.create(value.trim()).normalize();
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || uri.getUserInfo() != null
                    || !(ALLOWED_HOSTS.contains(host.toLowerCase(Locale.ROOT))
                    || host.toLowerCase(Locale.ROOT).endsWith(".koreafilm.or.kr"))) {
                throw new IllegalArgumentException("허용되지 않은 포스터 주소입니다.");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("유효하지 않거나 허용되지 않은 포스터 주소입니다.", exception);
        }
    }

    private boolean isAllowedImageType(String contentType) {
        return contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp");
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private void validateImageSignature(byte[] bytes, String contentType) {
        boolean valid = switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
        if (!valid) {
            throw new IllegalStateException("포스터 파일의 실제 이미지 형식이 응답 MIME 형식과 일치하지 않습니다.");
        }
    }

    private record DownloadedImage(byte[] bytes, String contentType) {}
}
