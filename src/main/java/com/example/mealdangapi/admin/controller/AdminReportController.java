package com.example.mealdangapi.admin.controller;

import com.example.mealdangapi.admin.service.AdminReportService;
import com.example.mealdangapi.board.dto.response.ReportedPostResponse;
import com.example.mealdangapi.global.common.ApiResponse;
import com.example.mealdangapi.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports/posts" )
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * PENDING 신고 건수가 기준 이상인 게시글 목록을 조회한다.
     * 관리자 여부는 서비스에서 JWT 이메일 기준으로 검증한다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportedPostResponse>>>
    getReportedPosts(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int minReportCount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ReportedPostResponse> response =
                adminReportService.getReportedPosts(
                        authentication.getName(),
                        minReportCount,
                        page,
                        size
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 신고 수락 처리.
     * 종선 도메인: PENDING 신고 전체 ACCEPTED, 게시글 HIDDEN.
     * 치연 도메인: 연결 레시피 비활성화, 관리자 감사 로그 기록.
     */
    @PatchMapping("/{postId}/accept")
    public ResponseEntity<ApiResponse<AdminReportService.ReportActionResponse>>
    acceptReport(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestBody(required = false) ReportActionRequest request
    ) {
        AdminReportService.ReportActionResponse response =
                adminReportService.acceptReport(
                        authentication.getName(),
                        postId,
                        request == null ? null : request.memo()
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 신고 기각 처리.
     * 종선 도메인: PENDING 신고 전체 DISMISSED.
     * 게시글 상태와 연결 레시피 상태는 유지한다.
     */
    @PatchMapping("/{postId}/dismiss")
    public ResponseEntity<ApiResponse<AdminReportService.ReportActionResponse>>
    dismissReport(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestBody(required = false) ReportActionRequest request
    ) {
        AdminReportService.ReportActionResponse response =
                adminReportService.dismissReport(
                        authentication.getName(),
                        postId,
                        request == null ? null : request.memo()
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 메모가 필요 없으면 요청 본문 없이 호출해도 된다.
     */
    public record ReportActionRequest(
            String memo
    ) {
    }
}
