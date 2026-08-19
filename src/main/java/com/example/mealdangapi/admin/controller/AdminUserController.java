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
     * 프론트 관리자 화면의 7일·30일 정지 처리.
     * 요청값에는 정지 대상 userId와 기간만 사용하며,
     * 실제 관리자 ID는 JWT Authentication에서만 추출한다.
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
     * 관리자의 조기 정지 해제 기능.
     * 프론트에 현재 버튼은 없지만, 정지 처리 실수를 복구할 운영 API로 필요하다.
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
     * 프론트 관리자 화면에서 대상 사용자의 실효 정지 상태를 표시할 때 사용한다.
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
            String memo
    ) {
    }
}
