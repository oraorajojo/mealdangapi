package com.example.mealdangapi.board.entity;

/**
 * POST_REPORTS.REASON_CODE
 * DDL: ENUM('SPAM', 'ABUSE', 'ADULT', 'FALSE_INFO', 'COPYRIGHT', 'ETC') NOT NULL
 *
 * 와이어프레임 레시피 상세의 [신고] 버튼 → 팝업에서 사유를 고르는 항목이다.
 *
 * SPAM       : 스팸·광고
 * ABUSE      : 욕설·비방
 * ADULT      : 선정적 내용
 * FALSE_INFO : 허위 정보 (잘못된 조리법, 위험한 재료 조합 등)
 * COPYRIGHT  : 저작권 침해 (타 사이트 레시피·사진 무단 도용)
 * ETC        : 기타 — 이 값을 고르면 etc_reason 입력이 필수다
 *              (검증은 서비스에서. ErrorCode.REPORT_REASON_REQUIRED)
 *
 * ★ 상수명은 DB ENUM 문자열과 철자까지 일치해야 한다.
 *   @Enumerated(EnumType.STRING)이 이 이름을 그대로 저장하기 때문.
 */
public enum ReportReasonCode {
    SPAM,
    ABUSE,
    ADULT,
    FALSE_INFO,
    COPYRIGHT,
    ETC
}
