package com.momo.backend.dto.comment;

import com.momo.backend.entity.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentResponse {

    // 댓글 식별 번호
    private final Long commentId;

    // 댓글이 작성된 리뷰 번호
    private final Long reviewId;

    // 댓글 작성자 정보
    private final Long userId;
    private final String nickname;

    // 댓글 내용
    private final String content;

    // 댓글 작성 및 수정 시간
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // Comment Entity를 응답 DTO로 변환
    public CommentResponse(Comment comment) {
        this.commentId = comment.getCommentId();
        this.reviewId = comment.getReview().getReviewId();
        this.userId = comment.getUser().getUserId();
        this.nickname = comment.getUser().getNickname();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
    }
}