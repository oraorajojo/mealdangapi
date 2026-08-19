package com.example.mealdangapi.board.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * POST_LIKES의 복합 기본키 클래스.
 * DDL: PRIMARY KEY (user_id, post_id)
 *
 * 왜 별도 클래스가 필요한가:
 *   JPA는 엔티티 하나당 식별자가 하나여야 한다. 컬럼 두 개가 묶여 PK인 경우
 *   그 두 개를 담는 클래스를 따로 만들어 "이게 식별자다"라고 알려줘야 한다.
 *
 * ★ 규칙 3가지 (하나라도 어기면 기동 시점에 에러가 난다)
 *   ① Serializable을 구현할 것
 *   ② 파라미터 없는 기본 생성자가 있을 것
 *   ③ equals()와 hashCode()를 반드시 재정의할 것
 *
 *   ③이 특히 중요하다. JPA는 영속성 컨텍스트에서 "같은 엔티티인지"를 식별자의
 *   equals()로 판단한다. 재정의하지 않으면 기본 구현(주소 비교)이 쓰여서
 *   같은 (user_id, post_id)인데도 다른 객체로 취급돼 중복 조회·중복 INSERT가 생긴다.
 *
 * ★ 필드 이름은 PostLike 엔티티의 @Id 필드 이름과 정확히 일치해야 한다.
 *   (userId ↔ userId, postId ↔ postId)
 */
public class PostLikeId implements Serializable {

    private Long userId;
    private Long postId;

    /** JPA 전용 기본 생성자 (규칙 ②) */
    public PostLikeId() {
    }

    public PostLikeId(Long userId, Long postId) {
        this.userId = userId;
        this.postId = postId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPostId() {
        return postId;
    }

    /** 규칙 ③ — 두 값이 모두 같아야 같은 식별자 */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostLikeId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(postId, that.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, postId);
    }
}
