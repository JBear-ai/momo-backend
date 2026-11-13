package com.momo.backend.controller;

import com.momo.backend.dto.chat.ChatImageUploadResponse;
import com.momo.backend.entity.User;
import com.momo.backend.repository.UserRepository;
import com.momo.backend.service.ChatImageStorageService;
import com.momo.backend.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 인증된 회원의 채팅 이미지 업로드와 메시지 연결 이미지 조회를 담당한다. */
@RestController
@RequestMapping("/api/chat/images")
@RequiredArgsConstructor
public class ChatImageController {

    private final ChatImageStorageService imageStorageService;
    private final ChatMessageService chatMessageService;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatImageUploadResponse upload(Authentication authentication, @RequestPart("file") MultipartFile file) {
        User user = currentUser(authentication);
        return new ChatImageUploadResponse(imageStorageService.store(file, user.getUserId()));
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> getImage(Authentication authentication, @PathVariable String imageId) {
        currentUser(authentication);
        if (!chatMessageService.containsImage(imageId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅 이미지를 찾을 수 없습니다.");
        }
        ChatImageStorageService.ImageContent image = imageStorageService.read(imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff")
                .body(image.bytes());
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findByLoginId(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다."));
    }
}
