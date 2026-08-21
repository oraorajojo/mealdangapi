package com.example.mealdangapi.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 레시피 후기 요약. 레시피 상세 상단의 "신뢰 정보" 영역용.
 *
 * 와이어프레임
 *   후기 평점  4.6
 *   조리 완료  27회
 *
 * ※ "조리 완료 27회"는 현재 후기 수(reviewCount)로 내려준다.
 *   규칙사전 §10의 "이 메뉴 만들게요" 선택 기록은 CHEF_SELECTIONS에 별도 저장되며
 *   담당 영역이 다르다. 그 값을 써야 한다면 추후 협의해 변경한다.
 */
@Getter
@AllArgsConstructor
public class ReviewSummaryResponse {

    private Long recipeId;

    /**
     * 평균 별점. 소수점 첫째 자리까지 반올림한다. (예: 4.6)
     *
     * 후기가 하나도 없으면 0.0이 아니라 null을 내려준다.
     * 0.0으로 내리면 화면에 "0.0점"으로 표시되어
     * "평가가 낮은 레시피"처럼 보이기 때문이다.
     * 프론트에서 null이면 "아직 후기가 없어요"로 처리해주면 된다.
     */
    private Double averageRating;

    /** 후기 개수 */
    private long reviewCount;

    /**
     * 로그인 사용자가 이미 이 레시피에 후기를 썼는지.
     *
     * 규칙사전 §12 "회원당 레시피 1개만 작성" 이므로,
     * true면 작성 폼 대신 수정 폼을 보여줘야 한다.
     * 비로그인이면 항상 false.
     */
    private boolean written;

    /** 내가 쓴 후기의 ID. written이 false면 null */
    private Long myReviewId;
}
