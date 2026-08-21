package com.example.mealdangapi.fridge.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 냉장고 재료 등록·수정 요청.
 *
 * 와이어프레임 — 재료 이름 / 수량 / 단위 / 소비기한 / [＋ 등록]
 *
 * ★ "재료 이름"을 문자열로 받지 않고 ingredientId(숫자)를 받는다.
 *   user_fridge_items.ingredient_id가 FK라 표준 재료 ID여야만 저장된다.
 *
 *   프론트는 재료 검색 API(GET /api/ingredients/search?keyword=)로 자동완성 목록을 띄우고,
 *   사용자가 선택한 항목의 ingredientId를 여기 담아 보낸다.
 *   별칭 처리("달걀" → "계란")도 그 API가 담당한다.
 *
 *   규칙사전 §4-3 "인식하지 못한 단어는 임의의 재료로 확정하지 않는다"에 따라,
 *   검색으로 확정되지 않은 재료는 등록할 수 없다.
 */
@Getter
@Setter
@NoArgsConstructor
public class FridgeItemRequest {

    /** 재료 검색 API 응답의 ingredientId */
    @NotNull(message = "재료를 선택해주세요.")
    private Long ingredientId;

    /**
     * 수량. 선택 항목이다 — "김치 조금"처럼 수량을 모를 수 있다.
     *
     * DB에 CHECK (quantity IS NULL OR quantity > 0)이 걸려 있어 0 이하는 저장되지 않는다.
     * 여기서 먼저 막아 DB까지 가지 않게 한다.
     *
     * inclusive = false → 0은 허용하지 않고 0보다 커야 한다.
     * Digits는 DECIMAL(10,2) 범위를 넘지 않게 막는다.
     */
    @DecimalMin(value = "0", inclusive = false, message = "수량은 0보다 커야 합니다.")
    @Digits(integer = 8, fraction = 2, message = "수량 형식이 올바르지 않습니다.")
    private BigDecimal quantity;

    /** 단위(개, g, ml, 팩 등). 선택 항목 */
    @Size(max = 30, message = "단위는 30자 이하로 입력해주세요.")
    private String unit;

    /**
     * 소비기한. 선택 항목 — 와이어프레임의 "기한 미입력" 상태를 허용한다.
     *
     * 미래 날짜 제약(@FutureOrPresent)을 걸지 않은 이유:
     *   이미 지난 재료도 등록할 수 있어야 한다. 냉장고에 실제로 들어 있고,
     *   오히려 "지난 재료 1개"처럼 알려줘야 하는 대상이다.
     *
     * 형식: "2026-08-21" (ISO)
     */
    private LocalDate expiryDate;
}
