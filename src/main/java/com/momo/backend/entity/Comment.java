package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor
public class Comment {

    // 댓글을 구분하는 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    // 댓글이 작성된 리뷰
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // 댓글을 작성한 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 댓글 내용 (최대 500자)
    @Column(nullable = false, length = 500)
    private String content;

    // 댓글 작성 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 댓글을 마지막으로 수정한 시간
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 댓글 작성 시 회원, 리뷰, 내용을 전달받아 저장
    public Comment(Review review, User user, String content) {
        this.review = review;
        this.user = user;
        this.content = content;
    }

    @PrePersist
    public void prePersist() {
        // 처음 저장할 때 작성 시간과 수정 시간을 기록
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        // 댓글 수정 시 수정 시간을 갱신
        this.updatedAt = LocalDateTime.now();
    }

    // 기존 댓글 내용을 변경
    public void updateComment(String content) {
        this.content = content;
    }
}