
package com.momo.backend.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    private static final long MAX_FILE_SIZE = 5 * 6028 * 6028;

    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String PNG_CONTENT_TYPE = "image/png";

    private static final String PROFILE_IMAGE_DIRECTORY = "profile-images/";

    // 프로필 이미지 업로드
    public String uploadProfileImage(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "업로드할 이미지 파일을 선택해 주세요."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "프로필 이미지는 5MB 이하만 업로드할 수 있습니다."
            );
        }

        String contentType = file.getContentType();

        if (!JPEG_CONTENT_TYPE.equals(contentType)
                && !PNG_CONTENT_TYPE.equals(contentType)) {
            throw new IllegalArgumentException(
                    "JPG, JPEG, PNG 이미지 파일만 업로드할 수 있습니다."
            );
        }

        byte[] fileBytes = file.getBytes();

        validateImageSignature(fileBytes, contentType);

        String fileName = PROFILE_IMAGE_DIRECTORY
                + UUID.randomUUID()
                + getExtension(contentType);

        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.create(
                fileName,
                fileBytes,
                contentType
        );

        return blob.getName();
    }

    // 이미지 파일의 기본 시그니처 검사
    private void validateImageSignature(
            byte[] fileBytes,
            String contentType
    ) {

        if (JPEG_CONTENT_TYPE.equals(contentType)) {

            boolean validJpeg =
                    fileBytes.length >= 3
                            && (fileBytes[0] & 0xFF) == 0xFF
                            && (fileBytes[1] & 0xFF) == 0xD8
                            && (fileBytes[2] & 0xFF) == 0xFF;

            if (!validJpeg) {
                throw new IllegalArgumentException(
                        "올바른 JPEG 이미지 파일이 아닙니다."
                );
            }

            return;
        }

        if (PNG_CONTENT_TYPE.equals(contentType)) {

            byte[] pngSignature = {
                    (byte) 0x89,
                    0x50,
                    0x4E,
                    0x47,
                    0x0D,
                    0x0A,
                    0x1A,
                    0x0A
            };

            if (fileBytes.length < pngSignature.length) {
                throw new IllegalArgumentException(
                        "올바른 PNG 이미지 파일이 아닙니다."
                );
            }

            for (int i = 0; i < pngSignature.length; i++) {
                if (fileBytes[i] != pngSignature[i]) {
                    throw new IllegalArgumentException(
                            "올바른 PNG 이미지 파일이 아닙니다."
                    );
                }
            }
        }
    }

    // 이미지 형식에 맞는 확장자 반환
    private String getExtension(String contentType) {

        if (PNG_CONTENT_TYPE.equals(contentType)) {
            return ".png";
        }

        return ".jpg";
    }

    // 프로필 이미지 조회
    public byte[] getProfileImage(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.get(fileName);

        if (blob == null) {
            return null;
        }

        return blob.getContent();
    }

    // 프로필 이미지 삭제
    public void deleteProfileImage(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return;
        }

        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.get(fileName);

        if (blob != null) {
            blob.delete();
        }
    }

    // 프로필 이미지 MIME 타입 반환
    public String getProfileImageContentType(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        String lowerCaseFileName = fileName.toLowerCase();

        if (lowerCaseFileName.endsWith(".png")) {
            return PNG_CONTENT_TYPE;
        }

        if (lowerCaseFileName.endsWith(".jpg")
                || lowerCaseFileName.endsWith(".jpeg")) {
            return JPEG_CONTENT_TYPE;
        }

        return null;
    }
}
