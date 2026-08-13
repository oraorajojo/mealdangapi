package com.example.mealdangapi.board.entity;

/**
 * POST_REPORTS.STATUS
 * DDL: ENUM('PENDING', 'ACCEPTED', 'DISMISSED') NOT NULL DEFAULT 'PENDING'
 *
 * PENDING   : 접수됨. 아직 관리자가 검토하지 않음
 * ACCEPTED  : 신고 인정 → 게시글 HIDDEN + 레시피 is_active=FALSE
 * DISMISSED : 신고 기각 → 게시글은 그대로 유지
 *
 * ★ PENDING → ACCEPTED 전환은 hidePost() 안에서 처리한다.
 *   협의 문서에 이 항목이 빠져 있었는데, post_reports가 종선 테이블이므로
 *   치연이 아니라 내가 처리하는 게 맞다. 안 하면 이미 처리된 신고가 계속
 *   PENDING으로 남아 관리자 페이지에 반복 노출된다.
 *
 * ★ 확정 사항: 신고 10건 누적 시 "자동 숨김"이 아니라 "관리자 검토 대상"이 된다.
 *   10건이 되어도 상태는 PENDING이고, 관리자가 인정해야 ACCEPTED로 바뀐다.
 */
public enum ReportStatus {
    PENDING,
    ACCEPTED,
    DISMISSED
}
