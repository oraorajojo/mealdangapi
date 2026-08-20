package com.example.mealdangapi.board.dto.response;

import com.example.mealdangapi.board.entity.PostStatus;
import com.example.mealdangapi.recipe.entity.ChefCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 신고 목록 조회 결과를 담는 중간 객체 (JPQL 프로젝션 전용)
 *
 * 신고 사유 요약은 별도 집계 쿼리로 가져와 서비스에서 합치므로 여기 포함되지 않는다.
 *
 * ★ 생성자의 파라미터 순서·타입은 JPQL의 new 표현식과 정확히 일치해야 한다.
 *   어긋나면 기동 시점이 아니라 쿼리 실행 시점에 에러가 난다.
 */
@Getter
public class ReportedPostRow {

    private final Long postId;
    private final Long recipeId;
    private final String title;
    private final ChefCode chefCode;
    private final PostStatus status;

    /** board_posts.report_count — 기각 여부와 무관한 누적 신고 횟수 */
    private final int totalReportCount;

    /** 아직 처리되지 않은(PENDING) 신고 건수 */
    private final long pendingReportCount;

    /** 가장 최근 신고 시각 */
    private final LocalDateTime lastReportedAt;

    public ReportedPostRow(
            Long postId,
            Long recipeId,
            String title,
            ChefCode chefCode,
            PostStatus status,
            int totalReportCount,
            long pendingReportCount,
            LocalDateTime lastReportedAt
    ) {
        this.postId = postId;
        this.recipeId = recipeId;
        this.title = title;
        this.chefCode = chefCode;
        this.status = status;
        this.totalReportCount = totalReportCount;
        this.pendingReportCount = pendingReportCount;
        this.lastReportedAt = lastReportedAt;
    }
}
