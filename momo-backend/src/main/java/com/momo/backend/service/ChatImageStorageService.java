package com.momo.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// 채팅 이미지 파일 검증·저장·정리
@Service
@Slf4j
public class ChatImageStorageService {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long PENDING_UPLOAD_TTL_SECONDS = 30 * 60;
    private static final int MAX_IMAGE_PIXELS = 20_000_000;
    private static final String FILE_PATTERN = "[0-9a-f-]{36}\\.(jpg|png|webp)";

    private final Path imageDirectory;
    private final Map<String, StoredImage> images = new ConcurrentHashMap<>();

    public ChatImageStorageService(
            @Value("${momo.storage.chat-image-dir:${user.home}/momo-data/chat-images}") String directory) {
        this.imageDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    // 메시지 이력이 메모리에만 있어 새 업로드 시 연결되지 않은 파일을 정리한다.
    private void removeOrphanedFiles() {
        try {
            Files.createDirectories(imageDirectory);
            var livePaths = images.values().stream().map(image -> image.path().toAbsolutePath().normalize()).toList();
            try (var files = Files.list(imageDirectory)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().matches(FILE_PATTERN))
                        .filter(path -> !livePaths.contains(path.toAbsolutePath().normalize()))
                        .forEach(this::deleteFileQuietly);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("채팅 이미지 저장소를 초기화할 수 없습니다.", exception);
        }
    }

    // 파일 이름이나 MIME 값 대신 실제 데이터와 크기를 확인해 저장한다.
    public synchronized String store(MultipartFile file, Long ownerId) {
        purgeExpiredUploads();
        removeOrphanedFiles();
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 선택해 주세요.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "이미지는 최대 5MB까지 업로드할 수 있습니다.");
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "이미지는 최대 5MB까지 업로드할 수 있습니다.");
            }
            ImageFormat format = detectAndValidate(bytes);
            String imageId = UUID.randomUUID().toString();
            Path destination = resolve(imageId, format.extension());
            Files.createDirectories(imageDirectory);
            Path temporary = Files.createTempFile(imageDirectory, "upload-", ".tmp");
            try {
                Files.write(temporary, bytes);
                try {
                    Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, destination);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            images.put(imageId, new StoredImage(ownerId, destination, format.contentType(), Instant.now(), false));
            return imageId;
        } catch (IOException exception) {
            throw new IllegalStateException("채팅 이미지 저장에 실패했습니다.", exception);
        }
    }

    // 업로드한 회원만 이미지를 메시지에 한 번 연결할 수 있다.
    public synchronized void attachToMessage(String imageId, Long ownerId) {
        StoredImage image = images.get(imageId);
        if (image != null && !image.attached()
                && image.createdAt().isBefore(Instant.now().minusSeconds(PENDING_UPLOAD_TTL_SECONDS))) {
            delete(imageId);
            image = null;
        }
        if (image == null || !image.ownerId().equals(ownerId) || image.attached()) {
            throw new ChatRuleViolationException("IMAGE_UNAVAILABLE", "이미지를 사용할 수 없거나 업로드한 회원이 아닙니다.");
        }
        images.put(imageId, image.withAttached(true));
    }

    // 현재 채팅 이력에 연결된 이미지 파일을 반환한다.
    public ImageContent read(String imageId) {
        StoredImage image = images.get(imageId);
        if (image == null || !image.attached()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅 이미지를 찾을 수 없습니다.");
        }
        try {
            return new ImageContent(Files.readAllBytes(image.path()), image.contentType());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅 이미지를 찾을 수 없습니다.");
        }
    }

    // 메시지가 삭제되거나 보관 한도를 넘으면 연결된 파일도 정리한다.
    public synchronized void delete(String imageId) {
        StoredImage image = images.remove(imageId);
        if (image != null) {
            deleteFileQuietly(image.path());
        }
    }

    private void purgeExpiredUploads() {
        Instant cutoff = Instant.now().minusSeconds(PENDING_UPLOAD_TTL_SECONDS);
        images.forEach((imageId, image) -> {
            if (!image.attached() && image.createdAt().isBefore(cutoff)) {
                delete(imageId);
            }
        });
    }

    private ImageFormat detectAndValidate(byte[] bytes) {
        ImageFormat format;
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            format = new ImageFormat("image/jpeg", ".jpg");
        } else if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            format = new ImageFormat("image/png", ".png");
        } else if (isWebp(bytes)) {
            format = new ImageFormat("image/webp", ".webp");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPEG, PNG, WebP 이미지만 업로드할 수 있습니다.");
        }

        // JPEG·PNG는 이미지 디코더로 확인하고, 처리량이 큰 이미지는 거부한다.
        if (!format.contentType().equals("image/webp")) {
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않거나 해상도가 너무 큰 이미지입니다.");
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width < 1 || height < 1 || (long) width * height > MAX_IMAGE_PIXELS
                            || reader.read(0) == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않거나 해상도가 너무 큰 이미지입니다.");
                    }
                } finally {
                    reader.dispose();
                }
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 데이터를 읽을 수 없습니다.");
            }
        }
        return format;
    }

    private boolean isWebp(byte[] bytes) {
        if (bytes.length < 20 || !ascii(bytes, 0, "RIFF") || !ascii(bytes, 8, "WEBP")) {
            return false;
        }
        long riffSize = Integer.toUnsignedLong(
                (bytes[4] & 0xFF) | ((bytes[5] & 0xFF) << 8) | ((bytes[6] & 0xFF) << 16) | (bytes[7] << 24));
        if (riffSize != bytes.length - 8L) return false;

        int offset = 12;
        boolean hasImageChunk = false;
        while (offset + 8 <= bytes.length) {
            long chunkSize = Integer.toUnsignedLong(
                    (bytes[offset + 4] & 0xFF) | ((bytes[offset + 5] & 0xFF) << 8)
                            | ((bytes[offset + 6] & 0xFF) << 16) | (bytes[offset + 7] << 24));
            if (chunkSize > bytes.length - offset - 8L) return false;
            if (ascii(bytes, offset, "VP8 ") || ascii(bytes, offset, "VP8L") || ascii(bytes, offset, "VP8X")) {
                int minimumChunkSize = ascii(bytes, offset, "VP8 ") ? 10 : ascii(bytes, offset, "VP8L") ? 5 : 10;
                if (chunkSize < minimumChunkSize) return false;
                hasImageChunk = true;
            }
            offset += 8 + (int) chunkSize + ((chunkSize & 1) == 1 ? 1 : 0);
        }
        return offset == bytes.length && hasImageChunk;
    }

    private boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[index] & 0xFF) != signature[index]) return false;
        }
        return true;
    }

    private boolean ascii(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int index = 0; index < value.length(); index++) {
            if (bytes[offset + index] != value.charAt(index)) return false;
        }
        return true;
    }

    private Path resolve(String imageId, String extension) {
        Path path = imageDirectory.resolve(imageId + extension).normalize();
        if (!path.startsWith(imageDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 이미지 식별자입니다.");
        }
        return path;
    }

    private void deleteFileQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("채팅 이미지 파일 정리에 실패했습니다.");
        }
    }

    private record ImageFormat(String contentType, String extension) { }
    private record StoredImage(Long ownerId, Path path, String contentType, Instant createdAt, boolean attached) {
        private StoredImage withAttached(boolean value) {
            return new StoredImage(ownerId, path, contentType, createdAt, value);
        }
    }

    public record ImageContent(byte[] bytes, String contentType) { }
}
