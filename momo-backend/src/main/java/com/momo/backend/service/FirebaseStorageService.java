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

    // 프로필 이미지를 Firebase Storage에 업로드합니다.
    public String uploadProfileImage(MultipartFile file) throws IOException {

        String contentType = file.getContentType();

        // JPG, JPEG, PNG 이미지 파일만 허용합니다.
        if (contentType == null ||
                (!contentType.equals("image/jpeg")
                        && !contentType.equals("image/png"))) {
            throw new IllegalArgumentException(
                    "JPG, JPEG, PNG 이미지 파일만 업로드할 수 있습니다."
            );
        }

        // 프로필 이미지의 최대 크기는 5MB로 제한합니다.
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "프로필 이미지는 5MB 이하만 업로드할 수 있습니다."
            );
        }

        // 같은 이름의 파일이 충돌하지 않도록 UUID로 저장 파일명을 생성합니다.
        String fileName =
                "profile-images/" + UUID.randomUUID() + getExtension(contentType);

        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.create(
                fileName,
                file.getBytes(),
                contentType
        );

        return blob.getName();
    }

    // 이미지 형식에 맞는 파일 확장자를 반환합니다.
    private String getExtension(String contentType) {
        if ("image/png".equals(contentType)) {
            return ".png";
        }

        return ".jpg";
    }

    // Firebase Storage에 저장된 프로필 이미지 데이터를 가져옵니다.
    public byte[] getProfileImage(String fileName) {

        // 저장된 이미지 경로가 없는 경우 조회하지 않습니다.
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.get(fileName);

        // Firebase Storage에 파일이 없는 경우 null을 반환합니다.
        if (blob == null) {
            return null;
        }

        return blob.getContent();
    }

    // Firebase Storage에 저장된 프로필 이미지를 삭제합니다.
    public void deleteProfileImage(String fileName) {

        // 저장된 이미지 경로가 없으면 삭제하지 않습니다.
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        Bucket bucket = StorageClient.getInstance().bucket();
        Blob blob = bucket.get(fileName);

        // 실제 파일이 존재하는 경우에만 삭제합니다.
        if (blob != null) {
            blob.delete();
        }
    }

    // 저장된 이미지 경로를 기준으로 이미지 형식을 반환합니다.
    public String getProfileImageContentType(String fileName) {

        if (fileName != null && fileName.toLowerCase().endsWith(".png")) {
            return "image/png";
        }

        return "image/jpeg";
    }
}