package com.example.mealdangapi.admin.dto;

import java.time.LocalDateTime;

/**
 * 관리자 "신고 유저 정지" 목록의 행 하나. ADMIN 역할은 목록에서 아예 제외된
 * 상태로 내려온다(쿼리 단계에서 필터링).
 */
public record AdminUserSummaryResponse(
        Long userId,
        String email,
        String nickname,
        String status,
        long reportCount,
        LocalDateTime createdAt
) {
    public static AdminUserSummaryResponse of(AdminUserListRow row) {
        return new AdminUserSummaryResponse(
                row.getUserId(),
                row.getEmail(),
                row.getNickname(),
                row.getStatus() != null ? row.getStatus().name() : null,
                row.getReportCount() != null ? row.getReportCount() : 0,
                row.getCreatedAt()
        );
    }
}
