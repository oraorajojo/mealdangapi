package com.example.mealdangapi.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 게시글 좋아요 (POST_LIKES) — 담당: 종선
 *
 * 규칙사전 §12 — "게시글 좋아요: 회원당 게시글 1회만 가능"
 *   → 이 제약을 PK (user_id, post_id)로 DB가 직접 보장한다.
 *     별도 UNIQUE 인덱스가 없는 이유가 이것. PK 자체가 중복을 막는다.
 *
 * ★ 좋아요는 "수정"이 없는 엔티티다. 누르면 INSERT, 취소하면 DELETE.
 *   그래서 updated_at 컬럼도 없고, 상태를 바꾸는 메서드도 두지 않는다.
 *
 * ★ 좋아요 INSERT/DELETE는 반드시 BOARD_POSTS.like_count 증감과 같은 트랜잭션이어야 한다.
 *   둘이 갈라지면 실제 좋아요 수와 화면에 보이는 숫자가 어긋난다.
 *   (설계정의서 §12 검수 항목 "동시성 검증")
 */
@Entity
@Table(name = "post_likes")
@IdClass(PostLikeId.class)
// @IdClass vs @EmbeddedId — 둘 다 복합키를 다루는 방식이다.
//   @IdClass  : 엔티티에 필드를 그대로 두고, 별도 ID 클래스로 식별자만 선언 (지금 방식)
//   @EmbeddedId: 엔티티가 ID 객체를 필드로 품는 방식
// @IdClass를 택한 이유는 postLike.getUserId()처럼 평평하게 접근할 수 있어서다.
// @EmbeddedId면 postLike.getId().getUserId()가 되어 한 단계 더 들어가야 한다.
public class PostLike {

    /**
     * 좋아요를 누른 회원. FK → users.user_id, ON DELETE CASCADE
     *
     * CASCADE인 이유: 회원이 탈퇴하면 그 사람이 누른 좋아요는 남아 있을 이유가 없다.
     * (반면 신고 기록은 RESTRICT라 남는다 — 운영 이력이라 보존해야 하므로)
     *
     * @Id가 두 개인 게 정상이다. @IdClass가 이 둘을 묶어 하나의 식별자로 취급한다.
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * 대상 게시글. FK → board_posts.post_id, ON DELETE CASCADE
     * 게시글이 물리 삭제되면 좋아요도 함께 사라진다.
     * (다만 우리는 게시글을 소프트 삭제(status=DELETED)로만 처리하므로 실제로는 잘 발생하지 않는다)
     */
    @Id
    @Column(name = "post_id")
    private Long postId;

    /**
     * updated_at이 없다. 좋아요는 생성/삭제만 있고 수정이 없기 때문.
     * DDL에도 created_at 하나뿐이다.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** JPA 전용 */
    protected PostLike() {
    }

    private PostLike(Long userId, Long postId) {
        this.userId = Objects.requireNonNull(userId, "userId는 필수입니다.");
        this.postId = Objects.requireNonNull(postId, "postId는 필수입니다.");
    }

    /**
     * 좋아요 생성.
     *
     * 서비스에서는 이 앞에 existsById()로 중복을 먼저 확인하지만,
     * 동시 요청이 검사와 INSERT 사이를 비집고 들어오면 PK 중복으로 예외가 난다.
     * 그건 GlobalExceptionHandler의 DataIntegrityViolationException 핸들러가 409로 받아준다.
     */
    public static PostLike of(Long userId, Long postId) {
        return new PostLike(userId, postId);
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPostId() {
        return postId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
