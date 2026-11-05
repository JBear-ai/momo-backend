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

    private static final String STORAGE_BUCKET = "momo-ca07b.firebasestorage.app";

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            // 프로젝트 내에 있는 서비스 계정 JSON 파일의 절대 경로를 직접 지정
            String filePath = "D:\\dev\\MOMO\\momo-backend\\src\\main\\resources\\firebase-adminsdk.json";

            try (FileInputStream serviceAccount = new FileInputStream(filePath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(STORAGE_BUCKET)
                        .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new RuntimeException("Firebase application initialization failed", e);
        }
    }
}