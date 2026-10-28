
package com.momo.backend.controller;

import com.momo.backend.service.CommentReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class CommentReactionController {

    // 댓글 좋아요/싫어요 기능을 처리하는 Service
    private final CommentReactionService reactionService;

    // 로그인한 사용자의 댓글 좋아요/싫어요 저장 및 변경
    @PutMapping("/{commentId}/reaction")
    public ResponseEntity<Void> saveReaction(
            @PathVariable Long commentId,
            @RequestBody Boolean liked,
            Authentication authentication) {

        reactionService.saveReaction(
                authentication.getName(),
                commentId,
                liked
        );

        return ResponseEntity.ok().build();
    }

    // 로그인한 사용자의 댓글 반응 취소
    @DeleteMapping("/{commentId}/reaction")
    public ResponseEntity<Void> deleteReaction(
            @PathVariable Long commentId,
            Authentication authentication) {

        reactionService.deleteReaction(
                authentication.getName(),
                commentId
        );

        return ResponseEntity.noContent().build();
    }

    // 특정 댓글의 좋아요/싫어요 개수 조회
    @GetMapping("/{commentId}/reactions")
    public ResponseEntity<Map<String, Long>> getReactionCounts(
            @PathVariable Long commentId) {

        // Service에서 좋아요와 싫어요 개수 조회
        long[] counts = reactionService.getReactionCounts(commentId);

        // 조회한 결과를 JSON 형태로 반환
        Map<String, Long> response = new HashMap<>();
        response.put("commentId", commentId);
        response.put("likeCount", counts[0]);
        response.put("dislikeCount", counts[1]);

        return ResponseEntity.ok(response);
    }
}
