package com.example.mealdangapi.fridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 냉장고 요약 집계.
 *
 * 와이어프레임 상단 카드
 *   재료 12개 / 지남 1 / 3일 이내 3 / 여유 있음 8
 *
 * ★ 합계 관계
 *   totalCount = expiredCount + imminentCount + safeCount + noExpiryCount
 *
 *   와이어프레임은 3개 항목만 보여주는데, 기한 미입력 재료를 어디에 넣을지가 애매하다.
 *   서버는 4개로 분리해 내려주고, 화면에서 어떻게 묶을지는 프론트가 정한다.
 *   (예: "여유 있음"에 noExpiryCount를 합쳐 표시)
 */
@Getter
@AllArgsConstructor
public class FridgeSummaryResponse {

    /** 냉장고에 담긴 재료 총 개수 */
    private long totalCount;

    /** 소비기한이 지난 재료 */
    private long expiredCount;

    /** 오늘 포함 3일 이내 만료 예정 */
    private long imminentCount;

    /** 4일 이상 남음 */
    private long safeCount;

    /** 소비기한 미입력 */
    private long noExpiryCount;
}
