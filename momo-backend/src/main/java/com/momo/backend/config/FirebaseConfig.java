package com.momo.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() throws IOException {

        // 로컬에 보관한 Firebase 서비스 계정 키를 읽어옵니다.
        FileInputStream serviceAccount =
                new FileInputStream("momo-backend/config/firebase-service-account.json");

        // Firebase Admin SDK에서 사용할 인증 정보와 Storage 버킷을 설정합니다.
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