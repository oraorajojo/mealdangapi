package com.example.mealdangapi.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 후기 작성·수정 요청.
 *
 * 와이어프레임 — 레시피 상세 하단
 *   별점 선택 ☆☆☆☆☆ / 후기 입력 ______ / [후기 등록]
 *
 * 작성과 수정이 같은 형태라 DTO를 공용으로 쓴다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReviewRequest {

    /**
     * 별점 1~5.
     *
     * 규칙사전 §12 "별점: 1~5점 정수만 허용"
     * DB에도 CHECK(rating BETWEEN 1 AND 5) 제약이 있지만,
     * 여기서 먼저 걸러야 DB까지 가서 터지는 걸 막을 수 있다.
     *
     * Integer(래퍼)로 받는 이유:
     *   int로 두면 값을 안 보냈을 때 0으로 채워져 @NotNull이 동작하지 않는다.
     *   "별점을 선택하지 않음"과 "0점"을 구분하려면 래퍼 타입이어야 한다.
     */
    @NotNull(message = "별점을 선택해주세요.")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    private Integer rating;

    /**
     * 후기 내용. 별점만 남기고 글은 안 쓸 수 있어 선택 항목이다.
     * (DB도 content NULL 허용)
     *
     * 1000자 제한은 DB 컬럼이 TEXT라 기술적 상한은 아니지만,
     * 지나치게 긴 후기는 화면이 깨지므로 서비스 정책으로 제한한다.
     */
    @Size(max = 1000, message = "후기는 1000자 이하로 입력해주세요.")
    private String content;
}
