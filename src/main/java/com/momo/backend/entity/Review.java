package com.momo.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_user_movie",
                        columnNames = {"user_id", "movie_code"}
                )
        }
)
@Getter
@NoArgsConstructor
public class Review {

    // 리뷰를 구분하는 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    // 리뷰를 작성한 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 리뷰 대상 영화의 KOBIS 식별 코드
    @Column(name = "movie_code", nullable = false, length = 20)
    private String movieCode;

    // 리뷰 제목
    @Column(nullable = false, length = 100)
    private String title;

    // 리뷰 본문
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 영화 평가 (true: 좋아요, false: 싫어요)
    @Column(nullable = false)
    private Boolean liked;

    // 리뷰 작성 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 리뷰를 마지막으로 수정한 시간
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 리뷰 작성 시 필요한 정보를 전달받아 저장
    public Review(User user, String movieCode, String title,
                  String content, Boolean liked) {
        this.user = user;
        this.movieCode = movieCode;
        this.title = title;
        this.content = content;
        this.liked = liked;
    }

    @PrePersist
    public void prePersist() {
        // 처음 저장할 때 작성 시간과 수정 시간을 동일하게 기록
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        // 리뷰 내용이 수정될 때 수정 시간을 갱신
        this.updatedAt = LocalDateTime.now();
    }

    // 기존 리뷰의 제목, 내용, 평점을 변경
    public void updateReview(String title, String content, Boolean liked) {
        this.title = title;
        this.content = content;
        this.liked = liked;
    }
}