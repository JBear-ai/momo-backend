
package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "comment_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_comment_reaction_user_comment",
                        columnNames = {"user_id", "comment_id"}
                )
        }
)
@Getter
@NoArgsConstructor
public class CommentReaction {

    // 댓글 반응 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long reactionId;

    // 반응을 남긴 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 반응 대상 댓글
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    // 댓글 반응 (true: 좋아요, false: 싫어요)
    @Column(nullable = false)
    private Boolean liked;

    // 댓글 반응 저장
    public CommentReaction(User user, Comment comment, Boolean liked) {
        this.user = user;
        this.comment = comment;
        this.liked = liked;
    }

    // 기존 반응 변경
    public void updateReaction(Boolean liked) {
        this.liked = liked;
    }
}
