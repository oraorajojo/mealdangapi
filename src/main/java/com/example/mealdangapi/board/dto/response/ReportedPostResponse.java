package com.example.mealdangapi.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 신고 목록의 게시글 1건.
 *
 * 와이어프레임 관리자 페이지 — "게시판 관리 (신고 몇회 이상)"
 *   냉동만두 덮밥 · 팬하나 · 2회 · [제재] [확인]
 */
@Getter
@AllArgsConstructor
public class ReportedPostResponse {

    private Long postId;

    /**
     * 연결된 레시피 ID.
     * 신고 수락 시 이 레시피를 비활성화해야 한다.
     * (hidePost()가 반환하는 값과 동일하므로 미리 확인용으로도 쓸 수 있다)
     */
    private Long recipeId;

    private String title;

    /** KOREAN / CHINESE / WESTERN / ETC */
    private String chefCode;

    /** 게시글 현재 상태. PUBLISHED / HIDDEN / DELETED */
    private String status;

    /** 누적 신고 횟수 (기각된 건 포함). board_posts.report_count */
    private int totalReportCount;

    /** 미처리(PENDING) 신고 건수. 관리자가 검토해야 할 건수 */
    private long pendingReportCount;

    /** 신고 사유별 집계. 어떤 사유로 많이 신고됐는지 파악용 */
    private List<ReasonCount> reasons;

    /** 가장 최근 신고 시각 */
    private LocalDateTime lastReportedAt;

    public static ReportedPostResponse of(ReportedPostRow row, List<ReasonCount> reasons) {
        return new ReportedPostResponse(
                row.getPostId(),
                row.getRecipeId(),
                row.getTitle(),
                row.getChefCode() != null ? row.getChefCode().name() : null,
                row.getStatus() != null ? row.getStatus().name() : null,
                row.getTotalReportCount(),
                row.getPendingReportCount(),
                reasons,
                row.getLastReportedAt()
        );
    }

    /**
     * 신고 사유별 건수.
     *
     * 예: [{ "reasonCode": "SPAM", "count": 7 }, { "reasonCode": "ABUSE", "count": 3 }]
     *
     * reasonCode 값은 SPAM / ABUSE / ADULT / FALSE_INFO / COPYRIGHT / ETC 이며,
     * 화면 표시 문구는 프론트에서 매핑한다.
     */
    @Getter
    @AllArgsConstructor
    public static class ReasonCount {
        private String reasonCode;
        private long count;
    }
}
