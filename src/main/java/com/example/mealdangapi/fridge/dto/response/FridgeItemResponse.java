package com.example.mealdangapi.fridge.dto.response;

import com.example.mealdangapi.fridge.entity.UserFridgeItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 냉장고 재료 1건.
 *
 * 와이어프레임 — 우유 / 1팩 / 2026-08-12 / D-1 / [수정] [삭제]
 */
@Getter
@AllArgsConstructor
public class FridgeItemResponse {

    private Long itemId;
    private Long ingredientId;

    /**
     * 표준 재료명.
     *
     * ingredientId만 내려주면 프론트가 화면에 숫자를 표시할 수 없어
     * 재료 조회 API를 항목 개수만큼 호출해야 한다. 서버에서 함께 조회해 내려준다.
     */
    private String ingredientName;

    /** 수량. 미입력이면 null */
    private BigDecimal quantity;

    /** 단위. 미입력이면 null */
    private String unit;

    /** 소비기한. 미입력이면 null */
    private LocalDate expiryDate;

    /**
     * 소비기한까지 남은 일수.
     *
     * 양수  : 아직 남음 (3 → 화면에 "D-3")
     * 0     : 오늘까지 ("D-DAY")
     * 음수  : 지남 (-2 → "2일 지남")
     * null  : 기한 미입력
     *
     * 서버에서 계산해 내려주는 이유:
     *   프론트에서 계산하면 사용자 기기의 시간대·시계 설정에 따라 값이 달라진다.
     *   서버 기준(Asia/Seoul)으로 통일한다.
     */
    private Long dDay;

    /**
     * 소비기한 상태. 프론트가 색상·정렬 등을 판단하는 값.
     *
     * EXPIRED   : 지남
     * IMMINENT  : 임박 (오늘 포함 3일 이내)
     * SAFE      : 여유 있음
     * NO_EXPIRY : 기한 미입력
     */
    private String expiryStatus;

    /** 소비기한 임박 판정 기준일 수. 와이어프레임의 "3일 이내"에 맞춘다 */
    private static final int IMMINENT_DAYS = 3;

    public static FridgeItemResponse of(
            UserFridgeItem item,
            String ingredientName,
            LocalDate today
    ) {
        Long dDay = null;
        String status = "NO_EXPIRY";

        if (item.getExpiryDate() != null) {
            // today → expiryDate 방향으로 센다. 미래면 양수, 과거면 음수.
            dDay = ChronoUnit.DAYS.between(today, item.getExpiryDate());

            if (dDay < 0) {
                status = "EXPIRED";
            } else if (dDay <= IMMINENT_DAYS) {
                status = "IMMINENT";
            } else {
                status = "SAFE";
            }
        }

        return new FridgeItemResponse(
                item.getItemId(),
                item.getIngredientId(),
                ingredientName,
                item.getQuantity(),
                item.getUnit(),
                item.getExpiryDate(),
                dDay,
                status
        );
    }
}
