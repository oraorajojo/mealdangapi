package com.example.mealdangapi.board.repository;

import com.example.mealdangapi.board.dto.response.ReportedPostRow;
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

    // ═══════════════════════════════════════════════════════════
    //  일반 조회
    // ═══════════════════════════════════════════════════════════

    /**
     * 중복 신고 확인.
     * UNIQUE (post_id, reporter_user_id)로 DB도 막지만,
     * 미리 확인해 명확한 에러 코드를 내려준다.
     */
    boolean existsByPostIdAndReporterUserId(Long postId, Long reporterUserId);

    /** 미처리 신고 목록 (관리자 페이지) */
    Page<PostReport> findAllByStatus(ReportStatus status, Pageable pageable);

    /** 특정 게시글의 신고 내역 */
    List<PostReport> findAllByPostIdOrderByCreatedAtDesc(Long postId);

    /** 특정 게시글의 상태별 신고 건수 */
    long countByPostIdAndStatus(Long postId, ReportStatus status);

    // ═══════════════════════════════════════════════════════════
    //  관리자 신고 목록 조회
    // ═══════════════════════════════════════════════════════════

    /**
     * 미처리 신고가 임계값 이상 쌓인 게시글 목록.
     *
     * ★ board_posts.report_count가 아니라 PENDING 신고 건수로 세는 이유
     *   report_count는 기각해도 줄지 않는 누적값이라, 한 번 10건이 넘은 게시글은
     *   관리자가 기각 처리해도 목록에 계속 남는다.
     *   "아직 검토가 필요한 게시글"을 보여주는 것이 목적이므로 PENDING 건수를 센다.
     *
     * ★ GROUP BY / HAVING 대신 스칼라 서브쿼리를 쓴 이유
     *   GROUP BY를 쓰면 페이징용 count 쿼리를 따로 만들기가 까다롭다.
     *   (HAVING이 걸린 결과의 행 수를 세야 하므로 중첩이 필요)
     *   서브쿼리 방식은 WHERE 조건이 그대로라 count 쿼리를 단순하게 유지할 수 있다.
     *
     * ★ FROM BoardPost p, Recipe r
     *   BoardPost가 Recipe를 @ManyToOne으로 매핑하지 않고 Long FK로 들고 있어
     *   (병합 충돌 회피를 위한 의도적 설계) JOIN 문법 대신 조건으로 연결한다.
     *
     *   주의: 이는 INNER JOIN이라 recipe_id가 NULL인 게시글은 조회되지 않는다.
     *   board_posts.recipe_id가 ON DELETE SET NULL이라 이론상 가능한 상태지만,
     *   레시피는 소프트 삭제(is_active=false)만 하므로 실제로는 발생하지 않는다.
     *
     * ★ r.active 조건을 걸지 않는다
     *   이미 비활성화된 레시피의 게시글도 관리자는 봐야 한다.
     *   (기각 후 되돌리는 경우 등)
     */
    @Query(
            value = "SELECT new com.example.mealdangapi.board.dto.response.ReportedPostRow("
                    + "  p.postId, p.recipeId, p.title, r.chefCode, p.status, p.reportCount, "
                    + "  (SELECT COUNT(r2) FROM PostReport r2 "
                    + "    WHERE r2.postId = p.postId "
                    + "      AND r2.status = com.example.mealdangapi.board.entity.ReportStatus.PENDING), "
                    + "  (SELECT MAX(r3.createdAt) FROM PostReport r3 "
                    + "    WHERE r3.postId = p.postId "
                    + "      AND r3.status = com.example.mealdangapi.board.entity.ReportStatus.PENDING)) "
                    + "FROM BoardPost p, Recipe r "
                    + "WHERE p.recipeId = r.recipeId "
                    + "  AND (SELECT COUNT(r4) FROM PostReport r4 "
                    + "        WHERE r4.postId = p.postId "
                    + "          AND r4.status = com.example.mealdangapi.board.entity.ReportStatus.PENDING) "
                    + "      >= :minReportCount "
                    + "ORDER BY p.reportCount DESC, p.postId DESC",
            countQuery = "SELECT COUNT(p) "
                    + "FROM BoardPost p, Recipe r "
                    + "WHERE p.recipeId = r.recipeId "
                    + "  AND (SELECT COUNT(r4) FROM PostReport r4 "
                    + "        WHERE r4.postId = p.postId "
                    + "          AND r4.status = com.example.mealdangapi.board.entity.ReportStatus.PENDING) "
                    + "      >= :minReportCount"
    )
    Page<ReportedPostRow> findReportedPosts(
            @Param("minReportCount") int minReportCount,
            Pageable pageable
    );

    /**
     * 여러 게시글의 신고 사유별 건수를 한 번에 조회.
     *
     * 게시글마다 개별 조회하면 목록 20건에 쿼리가 20번 나간다(N+1).
     * IN 절로 묶어 1번으로 처리한다.
     *
     * 반환 형태는 Object[] 배열의 리스트다.
     *   [0] = postId (Long)
     *   [1] = reasonCode (ReportReasonCode)
     *   [2] = 건수 (Long)
     * 한 게시글이 여러 사유로 신고되므로 같은 postId가 여러 번 나온다.
     */
    @Query("SELECT r.postId, r.reasonCode, COUNT(r) FROM PostReport r "
            + "WHERE r.postId IN :postIds "
            + "  AND r.status = com.example.mealdangapi.board.entity.ReportStatus.PENDING "
            + "GROUP BY r.postId, r.reasonCode "
            + "ORDER BY COUNT(r) DESC")
    List<Object[]> countReasonsByPostIds(@Param("postIds") List<Long> postIds);

    // ═══════════════════════════════════════════════════════════
    //  관리자 처리 — 일괄 상태 전환
    // ═══════════════════════════════════════════════════════════
    //
    // ★ 한 건씩 엔티티를 불러와 accept()/dismiss()를 호출하지 않는 이유
    //   한 게시글에 신고가 10건 이상 쌓여 있으므로 UPDATE가 10번 나간다.
    //   한 번의 UPDATE로 끝낸다.
    //
    // ★ WHERE에 status = PENDING 조건이 필요한 이유
    //   이미 처리된 건은 건드리지 않아야 한다.
    //   예전에 DISMISSED로 기각한 건이 나중에 ACCEPTED로 바뀌면 이력이 왜곡된다.

    /**
     * 게시글의 PENDING 신고를 일괄 ACCEPTED로 전환. (신고 수락)
     * hidePost()에서 호출한다.
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
     * 게시글의 PENDING 신고를 일괄 DISMISSED로 전환. (신고 기각)
     * dismissPendingReports()에서 호출한다.
     *
     * 게시글 상태와 레시피는 건드리지 않는다.
     *
     * @return 갱신된 신고 건수
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
