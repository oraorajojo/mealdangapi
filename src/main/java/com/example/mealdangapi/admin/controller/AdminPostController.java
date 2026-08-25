package com.example.mealdangapi.admin.controller;

import com.example.mealdangapi.admin.dto.AdminPostResponse;
import com.example.mealdangapi.admin.service.AdminPostService;
import com.example.mealdangapi.global.common.ApiResponse;
import com.example.mealdangapi.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 "전체 게시판" API — 회원 게시글 + 공공 API·관리자 등록 레시피를
 * 하나의 목록으로 훑어보는 용도. 신고된 것만 보여주는 AdminReportController와
 * 달리 활성 상태인 전체를 대상으로 한다.
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;

    /**
     * 전체 게시판 목록 조회 (관리자 전용).
     * GET /api/admin/posts?page=0&size=10
     *
     * 관리자 여부는 서비스에서 JWT 이메일 기준으로 검증한다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPostResponse>>> getAllPosts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<AdminPostResponse> response = adminPostService.getAllPosts(
                authentication.getName(),
                page,
                size
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
