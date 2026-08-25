package com.example.mealdangapi.admin.dto;

import com.example.mealdangapi.user.entity.UserStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 "신고 유저 정지" 전체 목록 조회 결과를 담는 중간 객체 (JPQL 프로젝션 전용).
 *
 * users 테이블엔 "신고당한 횟수"라는 컬럼이 없다 — 신고(post_reports)는 게시글
 * 단위라서, 이 유저가 쓴 게시글들의 report_count(board_posts, 누적 신고 수)를
 * 합산해 "이 유저가 신고당한 횟수"의 근사치로 쓴다.
 *
 * ★ 생성자의 파라미터 순서와 타입은 JPQL의 new 표현식과 정확히 일치해야 한다.
 */
@Getter
public class AdminUserListRow {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final UserStatus status;
    private final LocalDateTime createdAt;
    private final Long reportCount;

    public AdminUserListRow(
            Long userId,
            String email,
            String nickname,
            UserStatus status,
            LocalDateTime createdAt,
            Long reportCount
    ) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.createdAt = createdAt;
        this.reportCount = reportCount;
    }
}
