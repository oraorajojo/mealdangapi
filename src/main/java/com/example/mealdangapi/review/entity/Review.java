package com.example.mealdangapi.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 레시피 후기 (REVIEWS) — 담당: 종선
 *
 * 규칙사전 §12
 *   · 후기: 회원당 레시피 1개만 작성, 수정 가능  → UNIQUE (user_id, recipe_id)
 *   · 별점: 1~5점 정수만 허용                  → CHECK (rating BETWEEN 1 AND 5)
 *
 * ★ 후기는 게시글(board_posts)이 아니라 레시피(recipes)에 달린다.
 *   와이어프레임에서도 레시피 상세 화면 하단에 위치한다.
 *   게시판 카드의 📄 숫자는 이 테이블을 레시피 기준으로 집계한 값이다.
 *
 * ★ user_id, recipe_id 모두 ON DELETE RESTRICT다.
 *   후기가 달린 레시피는 물리 삭제가 DB 레벨에서 막힌다.
 *   (레시피는 is_active=false 소프트 삭제로만 처리하는 정책과 맞다)
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    /** 작성자. FK → users.user_id, ON DELETE RESTRICT */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /** 대상 레시피. FK → recipes.recipe_id, ON DELETE RESTRICT */
    @Column(name = "recipe_id", nullable = false, updatable = false)
    private Long recipeId;

    /**
     * 별점 1~5.
     * DB에 CHECK 제약이 있지만, 서비스에서 먼저 검증해
     * 명확한 에러 코드(INVALID_RATING)를 내려준다.
     */
    @Column(name = "rating", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private int rating;

    /** 후기 내용. 별점만 남기고 글은 안 쓸 수 있어 NULL 허용이다 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** JPA 전용 */
    protected Review() {
    }

    private Review(Long userId, Long recipeId, int rating, String content) {
        this.userId = Objects.requireNonNull(userId, "userId는 필수입니다.");
        this.recipeId = Objects.requireNonNull(recipeId, "recipeId는 필수입니다.");
        this.rating = rating;
        this.content = content;
    }

    public static Review of(Long userId, Long recipeId, int rating, String content) {
        return new Review(userId, recipeId, rating, content);
    }

    /**
     * 후기 수정. 규칙사전 §12 "회원당 레시피 1개만 작성, 수정 가능"
     * 작성자·대상 레시피는 바뀌지 않으므로 별점과 내용만 변경한다.
     */
    public void update(int rating, String content) {
        this.rating = rating;
        this.content = content;
    }

    /** 본인이 작성한 후기인지 확인 */
    public boolean isWrittenBy(Long userId) {
        return this.userId.equals(userId);
    }

    public Long getReviewId() {
        return reviewId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public int getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
