package com.example.mealdangapi.board.repository;

import com.example.mealdangapi.board.entity.PostReport;
import com.example.mealdangapi.board.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 신고 저장소 (POST_REPORTS) — 담당: 종선
 *
 * 확정 사항 — 신고 10건 누적 시 관리자 검토 대상(자동 숨김 아님).
 * 관리자가 인정하면 status=ACCEPTED + 게시글 HIDDEN + 레시피 is_active=FALSE가
 * 하나의 트랜잭션으로 처리된다. (협의자료 §4)
 */
public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    /**
     * 중복 신고 확인.
     *
     * UNIQUE (post_id, reporter_user_id)로 DB도 막고 있지만,
     * 미리 확인해서 409를 내려주는 게 사용자 입장에서 명확하다.
     * (DB까지 가서 터지면 어떤 제약이 걸렸는지 구분하기 어렵다)
     */
    boolean existsByPostIdAndReporterUserId(Long postId, Long reporterUserId);

    /**
     * 관리자 페이지 — 미처리 신고 목록.
     *
     * status=PENDING만 조회한다.
     * 인덱스: idx_post_reports_status (status, created_at)
     */
    Page<PostReport> findAllByStatus(ReportStatus status, Pageable pageable);

    /**
     * 특정 게시글의 신고 내역.
     *
     * 관리자가 "이 글이 왜 신고됐는지" 확인할 때 쓴다.
     * 사유별로 몇 건인지 보여주려면 이걸로 가져와서 서비스에서 집계한다.
     */
    List<PostReport> findAllByPostIdOrderByCreatedAtDesc(Long postId);

    /**
     * 특정 게시글의 미처리 신고 건수.
     *
     * 평소에는 board_posts.report_count 캐시를 쓰고 이건 대조·복구용이다.
     */
    long countByPostIdAndStatus(Long postId, ReportStatus status);

    /**
     * 게시글의 PENDING 신고를 한 번에 ACCEPTED로 전환.
     *
     * ★ hidePost()에서 호출한다. 협의 문서에 빠져 있던 항목이다.
     *   post_reports가 종선 테이블이므로 치연이 아니라 내가 처리한다.
     *   이걸 안 하면 처리된 신고가 계속 PENDING으로 남아 관리자 페이지에 반복 노출된다.
     *
     * 한 게시글에 신고가 여러 건 쌓여 있으므로(10건 이상) 하나씩 처리하지 않고
     * 한 번의 UPDATE로 끝낸다. 엔티티를 10개 불러와 각각 accept()를 부르면
     * UPDATE가 10번 나간다.
     *
     * WHERE에 status = PENDING 조건이 있어서, 이미 처리된 건은 건드리지 않는다.
     * (예전에 DISMISSED로 기각한 건이 다시 ACCEPTED가 되면 안 된다)
     *
     * @return 갱신된 신고 건수
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostReport r "
            + "SET r.status = com.example.mealdangapi.board.entity.ReportStatus.ACCEPTED, "
            + "    r.handledByAdminId = :adminId, "
            + "    r.handledAt = :handledAt "
            + "WHERE r.postId = :postId "
            + "  AND r.status = com.example.mealdangapi.board.entity.ReportStatus.PENDING")
    int acceptAllPendingByPostId(@Param("postId") Long postId,
                                 @Param("adminId") Long adminId,
                                 @Param("handledAt") LocalDateTime handledAt);

    /**
     * 게시글의 PENDING 신고를 한 번에 DISMISSED로 전환. (신고 기각)
     * 게시글 상태는 그대로 유지된다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostReport r "
            + "SET r.status = com.example.mealdangapi.board.entity.ReportStatus.DISMISSED, "
            + "    r.handledByAdminId = :adminId, "
            + "    r.handledAt = :handledAt "
            + "WHERE r.postId = :postId "
            + "  AND r.status = com.example.mealdangapi.board.entity.ReportStatus.PENDING")
    int dismissAllPendingByPostId(@Param("postId") Long postId,
                                  @Param("adminId") Long adminId,
                                  @Param("handledAt") LocalDateTime handledAt);
}
