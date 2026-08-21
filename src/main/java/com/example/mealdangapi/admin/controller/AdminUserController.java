package com.example.mealdangapi.admin.controller;

import com.example.mealdangapi.admin.service.AdminUserService;
import com.example.mealdangapi.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users" )
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 관리자 회원 정지 처리.
     * 7일, 30일, 영구 정지만 허용한다.
     * 실제 관리자 ID는 요청 본문이 아닌 JWT에서만 추출한다.
     */
    @PatchMapping("/{userId}/suspension")
    public ResponseEntity<ApiResponse<AdminUserService.SuspensionResponse>>
    suspendUser(
            Authentication authentication,
            @PathVariable Long userId,
            @RequestBody SuspensionRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminUserService.suspendUser(
                                authentication.getName(),
                                userId,
                                request
                        )
                )
        );
    }

    /**
     * 운영 중 잘못 정지된 계정을 관리자가 조기 해제한다.
     */
    @DeleteMapping("/{userId}/suspension")
    public ResponseEntity<ApiResponse<AdminUserService.SuspensionResponse>>
    activateUser(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminUserService.activateUser(
                                authentication.getName(),
                                userId
                        )
                )
        );
    }

    /**
     * 프론트 관리자 화면에서 대상 회원의 실효 정지 상태를 조회한다.
     */
    @GetMapping("/{userId}/suspension")
    public ResponseEntity<ApiResponse<AdminUserService.SuspensionStatusResponse>>
    getSuspensionStatus(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminUserService.getSuspensionStatus(
                                authentication.getName(),
                                userId
                        )
                )
        );
    }

    public record SuspensionRequest(
            Integer durationDays,
            Boolean permanent,
            String memo
    ) {
    }
}
