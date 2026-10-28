package com.momo.backend.controller;

import com.momo.backend.dto.comment.CommentRequest;
import com.momo.backend.dto.comment.CommentResponse;
import com.momo.backend.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins="http://localhost:3000")
@Slf4j
public class CommentController {

    // 댓글 작성, 조회, 수정, 삭제 기능을 처리하는 Service
    private final CommentService commentService;

    // 특정 리뷰에 댓글 작성
    @PostMapping("/api/reviews/{reviewId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long reviewId,
            @Valid @RequestBody CommentRequest dto,
            Authentication authentication) {

        CommentResponse response = commentService.createComment(
                reviewId,
                authentication.getName(),
                dto
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 특정 리뷰의 댓글 목록 조회
    @GetMapping("/api/reviews/{reviewId}/comments")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(
                commentService.getComments(reviewId, page)
        );
    }

    // 본인이 작성한 댓글 수정
    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest dto,
            Authentication authentication) {

        CommentResponse response = commentService.updateComment(
                commentId,
                authentication.getName(),
                dto
        );

        return ResponseEntity.ok(response);
    }

    // 본인이 작성한 댓글 삭제
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        commentService.deleteComment(
                commentId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}