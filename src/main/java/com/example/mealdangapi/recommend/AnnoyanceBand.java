package com.example.mealdangapi.recommend;

import java.math.BigDecimal;

/**
 * 귀찮음 구간 필터. 와이어프레임 문구("귀찮음 1~2 / 3 / 4~5") 기준으로 경계값을 잡았다.
 * annoyance_score가 DECIMAL(3,2)라 소수 둘째 자리까지만 존재하므로, X.99를 상한으로 써도
 * 그 사이 값(예: 2.995)은 애초에 저장될 수 없어 경계가 정확히 맞아떨어진다.
 */
public enum AnnoyanceBand {
    LOW(new BigDecimal("1.00"), new BigDecimal("2.99")),
    MID(new BigDecimal("3.00"), new BigDecimal("3.99")),
    HIGH(new BigDecimal("4.00"), new BigDecimal("5.00"));

    private final BigDecimal minScore;
    private final BigDecimal maxScore;

    AnnoyanceBand(BigDecimal minScore, BigDecimal maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public BigDecimal minScore() {
        return minScore;
    }

    public BigDecimal maxScore() {
        return maxScore;
    }
}
