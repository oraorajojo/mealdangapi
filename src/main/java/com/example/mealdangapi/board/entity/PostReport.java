package com.example.mealdangapi.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 게시글 신고 (POST_REPORTS) — 담당: 종선
 *
 * 확정 사항 — 신고 10건 누적 시 관리자 검토 대상이 된다(자동 숨김 아님).
 *   관리자가 인정하면 status=ACCEPTED + 게시글 HIDDEN + 레시피 is_active=FALSE가
 *   하나의 트랜잭션으로 처리된다. (협의자료 §4)
 *
 * 중복 신고 방지: UNIQUE (post_id, reporter_user_id)
 *   한 사람이 같은 글을 두 번 신고할 수 없다. 안 그러면 혼자서 10건을 채울 수 있다.
 */
@Entity
@Table(name = "post_reports")
public class PostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    /**
     * 신고 대상 게시글. FK → board_posts.post_id, ON DELETE CASCADE
     *
     * ※ 와이어프레임에서 [신고] 버튼은 레시피 상세 화면에 있는데,
     *   신고는 게시글(post_id) 단위로 저장된다. 따라서 레시피 상세 응답에
     *   postId가 포함되어야 프론트가 이 API를 호출할 수 있다.
     *   → 레시피 상세는 치연 담당이므로 응답에 postId 추가를 요청해야 한다.
     */
    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    /**
     * 신고자. FK → users.user_id, ON DELETE RESTRICT
     *
     * RESTRICT인 이유: 신고는 운영 이력이라 회원이 탈퇴해도 남아야 한다.
     * (좋아요는 CASCADE로 함께 지워지는 것과 대조적)
     */
    @Column(name = "reporter_user_id", nullable = false, updatable = false)
    private Long reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 20)
    private ReportReasonCode reasonCode;

    /**
     * 기타 사유 상세. reason_code=ETC일 때만 사용한다.
     *
     * DB는 NULL 허용이지만, ETC를 선택했는데 비어 있으면 서비스에서 막는다.
     * (ErrorCode.REPORT_REASON_REQUIRED)
     * 500자 제한은 DTO에 @Size(max = 500)으로 걸어 DB까지 가기 전에 잡는다.
     */
    @Column(name = "etc_reason", length = 500)
    private String etcReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    /** 처리한 관리자. FK → users.user_id, ON DELETE SET NULL */
    @Column(name = "handled_by_admin_id")
    private Long handledByAdminId;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** JPA 전용 */
    protected PostReport() {
    }

    private PostReport(Long postId, Long reporterUserId,
                       ReportReasonCode reasonCode, String etcReason) {
        this.postId = Objects.requireNonNull(postId, "postId는 필수입니다.");
        this.reporterUserId = Objects.requireNonNull(reporterUserId, "reporterUserId는 필수입니다.");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode는 필수입니다.");
        this.etcReason = etcReason;
        this.status = ReportStatus.PENDING;
    }

    /**
     * 신고 접수. 생성 시점의 상태는 항상 PENDING이다.
     *
     * @param etcReason reasonCode가 ETC가 아니면 null을 넘긴다
     */
    public static PostReport of(Long postId, Long reporterUserId,
                                ReportReasonCode reasonCode, String etcReason) {
        return new PostReport(postId, reporterUserId, reasonCode, etcReason);
    }

    // ─── 상태 변경 ────────────────────────────────────────────────
    // setStatus()를 열지 않는 이유는 BoardPost와 같다.
    // 상태를 바꿀 때 처리자·처리시각을 함께 기록해야 하는데,
    // setter로 열면 그걸 빠뜨리는 실수가 생긴다.

    /** 신고 인정. hidePost() 안에서 게시글 숨김과 함께 호출된다 */
    public void accept(Long adminId) {
        this.status = ReportStatus.ACCEPTED;
        this.handledByAdminId = adminId;
        this.handledAt = LocalDateTime.now();
    }

    /** 신고 기각. 게시글은 그대로 유지된다 */
    public void dismiss(Long adminId) {
        this.status = ReportStatus.DISMISSED;
        this.handledByAdminId = adminId;
        this.handledAt = LocalDateTime.now();
    }

    /**
     * 아직 처리되지 않은 신고인지.
     * 이미 ACCEPTED/DISMISSED인 건을 다시 처리하려 할 때 막는 용도.
     * (ErrorCode.REPORT_ALREADY_HANDLED)
     */
    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }

    public Long getReportId() {
        return reportId;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public ReportReasonCode getReasonCode() {
        return reasonCode;
    }

    public String getEtcReason() {
        return etcReason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public Long getHandledByAdminId() {
        return handledByAdminId;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
