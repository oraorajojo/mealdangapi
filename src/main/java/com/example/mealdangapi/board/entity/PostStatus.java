package com.example.mealdangapi.board.entity;

/**
 * BOARD_POSTS.STATUS
 * DDL: ENUM('PUBLISHED', 'HIDDEN', 'DELETED') NOT NULL DEFAULT 'PUBLISHED'
 *
 * PUBLISHED : 미식 연구소(게시판)에 노출되는 정상 상태
 * HIDDEN    : 신고 인정으로 관리자가 숨김. RECIPES.is_active=FALSE와 동일 트랜잭션 (§5.4)
 * DELETED   : 소프트 삭제. 물리 삭제하지 않는다 (설계 방침 "운영 이력 보존")
 *
 * ★ 상수명은 DB ENUM 문자열과 반드시 철자까지 일치해야 한다.
 *   @Enumerated(EnumType.STRING)이 이 이름을 그대로 DB에 쓰기 때문에,
 *   여기서 이름을 바꾸면 기존 행을 읽을 때 IllegalArgumentException이 난다.
 */
public enum PostStatus {
    PUBLISHED,
    HIDDEN,
    DELETED
}
