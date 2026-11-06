package com.momo.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() throws IOException {

        // 서비스 계정 키 파일을 읽은 뒤 사용이 끝나면 자동으로 닫습니다.
        try (FileInputStream serviceAccount =
                     new FileInputStream("./config/firebase-service-account.json")) {

            // Firebase Admin SDK 인증 정보와 Storage 버킷을 설정합니다.
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket("momo-ca07b.firebasestorage.app")
                    .build();

            // Firebase가 중복 초기화되지 않도록 최초 1회만 실행합니다.
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }
    }
}