
package com.momo.backend.controller;

import com.momo.backend.service.ReviewReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;


@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class ReviewReactionController {

    // 리뷰 좋아요/싫어요 기능을 처리하는 Service
    private final ReviewReactionService reactionService;

    // 로그인한 사용자의 리뷰 좋아요/싫어요 저장 및 변경
    @PutMapping("/{reviewId}/reaction")
    public ResponseEntity<Void> saveReaction(
            @PathVariable Long reviewId,
            @RequestBody Boolean liked,
            Authentication authentication) {

        reactionService.saveReactions(
                authentication.getName(),
                reviewId,
                liked
        );

        return ResponseEntity.ok().build();
    }

    // 로그인한 사용자의 리뷰 반응 취소
    @DeleteMapping("/{reviewId}/reaction")
    public ResponseEntity<Void> deleteReaction(
            @PathVariable Long reviewId,
            Authentication authentication) {

        reactionService.deleteReaction(
                authentication.getName(),
                reviewId
        );

        return ResponseEntity.noContent().build();
    }

    // 특정 리뷰의 좋아요/싫어요 개수 조회
    @GetMapping("/{reviewId}/reactions")
    public ResponseEntity<Map<String, Long>> getReactionCounts(
            @PathVariable Long reviewId) {

        // Service에서 좋아요와 싫어요 개수 조회
        long[] counts = reactionService.getReactionCounts(reviewId);

        // 조회한 결과를 JSON 형태로 반환
        Map<String, Long> response = new HashMap<>();
        response.put("reviewId", reviewId);
        response.put("likeCount", counts[0]);
        response.put("dislikeCount", counts[1]);

        return ResponseEntity.ok(response);
    }

}
