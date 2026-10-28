package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "review_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_reaction_user_review",
                        columnNames = {"user_id", "review_id"}
                )
        }
)
@Getter
@NoArgsConstructor
public class ReviewReactions {

    // 리뷰 반응 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long reactionId;

    // 반응을 남긴 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 반응 대상 리뷰
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // 리뷰 반응 (true: 좋아요, false: 싫어요)
    @Column(nullable = false)
    private Boolean liked;

    // 리뷰 반응 저장
    public ReviewReactions(User user, Review review, Boolean liked) {
        this.user = user;
        this.review = review;
        this.liked = liked;
    }

    // 기존 반응 변경
    public void updateReaction(Boolean liked) {
        this.liked = liked;
    }
}