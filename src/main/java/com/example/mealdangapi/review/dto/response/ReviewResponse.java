package com.example.mealdangapi.review.dto.response;

import com.example.mealdangapi.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 후기 1건.
 *
 * 와이어프레임 — ★★★★☆ 후기 내용 / 작성자 / 시간대
 */
@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long reviewId;
    private Long recipeId;
    private Long userId;

    /**
     * 작성자 닉네임.
     *
     * userId만 내려주면 프론트가 화면에 숫자를 표시할 수 없어
     * 회원 조회 API를 후기 개수만큼 호출해야 한다.
     * 서버에서 함께 조회해 내려주는 편이 낫다.
     */
    private String nickname;

    private int rating;
    private String content;

    /**
     * 내가 쓴 후기인지.
     * 수정·삭제 버튼을 보여줄지 판단하는 값이다.
     * 비로그인이면 항상 false.
     */
    private boolean mine;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse of(Review review, String nickname, boolean mine) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getRecipeId(),
                review.getUserId(),
                nickname,
                review.getRating(),
                review.getContent(),
                mine,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
