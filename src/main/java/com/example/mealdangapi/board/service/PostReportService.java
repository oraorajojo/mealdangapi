package com.example.mealdangapi.board.service;

import com.example.mealdangapi.board.dto.request.PostReportRequest;
import com.example.mealdangapi.board.dto.response.PostReportResponse;
import com.example.mealdangapi.board.entity.BoardPost;
import com.example.mealdangapi.board.entity.PostReport;
import com.example.mealdangapi.board.entity.PostStatus;
import com.example.mealdangapi.board.entity.ReportReasonCode;
import com.example.mealdangapi.board.repository.BoardPostRepository;
import com.example.mealdangapi.board.repository.PostReportRepository;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 신고 서비스 — 담당: 종선
 *
 * 확정 사항 — 신고 10건 누적 시 "자동 숨김"이 아니라 "관리자 검토 대상"이 된다.
 *   10건이 되어도 상태는 PENDING이고, 관리자가 인정해야 ACCEPTED로 바뀐다.
 *   실제 숨김 처리는 BoardPostCommandService.hidePost()에서 이루어진다.
 */
@Service
@RequiredArgsConstructor
public class PostReportService {

    private final PostReportRepository postReportRepository;
    private final BoardPostRepository boardPostRepository;

    /**
     * 신고 접수.
     *
     * POST_REPORTS INSERT와 BOARD_POSTS.report_count 증가가 같은 트랜잭션이다.
     */
    @Transactional
    public PostReportResponse report(
            Long reporterUserId,
            Long postId,
            PostReportRequest request
    ) {
        BoardPost post = boardPostRepository
                .findByPostIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 본인 글은 신고할 수 없다.
        // 없으면 자기 글을 스스로 신고해 노출을 조작할 수 있다.
        if (post.getUserId().equals(reporterUserId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "본인이 작성한 게시글은 신고할 수 없습니다."
            );
        }

        // 중복 신고 방지. UNIQUE (post_id, reporter_user_id)도 막고 있지만
        // 미리 확인해 명확한 에러 코드를 내려준다.
        if (postReportRepository
                .existsByPostIdAndReporterUserId(postId, reporterUserId)) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }

        String etcReason = normalizeEtcReason(
                request.getReasonCode(),
                request.getEtcReason()
        );

        PostReport report = postReportRepository.save(
                PostReport.of(
                        postId,
                        reporterUserId,
                        request.getReasonCode(),
                        etcReason
                )
        );

        // 원자적 UPDATE. 좋아요와 같은 이유로 엔티티 setter를 쓰지 않는다.
        boardPostRepository.increaseReportCount(postId);

        int reportCount = boardPostRepository.findById(postId)
                .map(BoardPost::getReportCount)
                .orElse(0);

        return new PostReportResponse(
                report.getReportId(),
                postId,
                reportCount,
                "신고가 접수되었습니다."
        );
    }

    /**
     * ETC 사유 검증 및 정리.
     *
     * ETC를 선택했으면 상세 사유가 필수다.
     * ETC가 아니면 상세 사유를 받아도 저장하지 않는다(null로 만든다).
     * 이 조건부 규칙은 @NotNull 같은 어노테이션으로 표현할 수 없어 여기서 처리한다.
     */
    private String normalizeEtcReason(ReportReasonCode reasonCode, String etcReason) {
        if (reasonCode == ReportReasonCode.ETC) {
            if (!StringUtils.hasText(etcReason)) {
                throw new BusinessException(ErrorCode.REPORT_REASON_REQUIRED);
            }
            return etcReason.trim();
        }

        return null;
    }
}