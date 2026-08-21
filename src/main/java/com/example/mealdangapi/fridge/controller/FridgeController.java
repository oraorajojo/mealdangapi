package com.example.mealdangapi.fridge.controller;

import com.example.mealdangapi.board.service.AuthenticatedUserResolver;
import com.example.mealdangapi.fridge.dto.request.FridgeItemRequest;
import com.example.mealdangapi.fridge.dto.response.FridgeItemResponse;
import com.example.mealdangapi.fridge.dto.response.FridgeSummaryResponse;
import com.example.mealdangapi.fridge.service.FridgeService;
import com.example.mealdangapi.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 나만의 냉장고 API — 담당: 종선
 *
 * ─── 인증 정책 ──────────────────────────────────────────────
 *   모든 API가 로그인 필수다. 개인 데이터라 비로그인으로 볼 수 있으면 안 된다.
 *
 * ★ 경로에 userId를 넣지 않는다.
 *   /api/users/{userId}/fridge 형태로 만들면 남의 userId를 넣어
 *   다른 사람의 냉장고를 조회할 수 있게 된다.
 *   사용자 식별은 토큰으로만 하고, 경로에는 노출하지 않는다.
 *
 * ★ SecurityConfig의 anyRequest().authenticated()에 걸려 자동으로 인증이 요구된다.
 *   별도 설정 추가가 필요 없다.
 */
@RestController
@RequestMapping("/api/fridge")
@RequiredArgsConstructor
public class FridgeController {

    private final FridgeService fridgeService;
    private final AuthenticatedUserResolver userResolver;

    /**
     * 내 냉장고 재료 목록 (소비기한 빠른 순)
     * GET /api/fridge/items
     */
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<FridgeItemResponse>>> getItems(
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(fridgeService.getItems(userId))
        );
    }

    /**
     * 냉장고 요약 (지남 / 3일 이내 / 여유 있음)
     * GET /api/fridge/summary
     *
     * 와이어프레임 상단 카드용.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<FridgeSummaryResponse>> getSummary(
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(fridgeService.getSummary(userId))
        );
    }

    /**
     * 소비기한 임박 재료 목록 (지난 것 포함)
     * GET /api/fridge/items/expiring
     *
     * 와이어프레임의 "임박 재료로 추천 →" 버튼이 이 목록을 사용한다.
     *
     * ※ 경로 순서 주의: /items/{itemId}보다 위에 선언되어야 한다.
     *   아래에 두면 "expiring"이 itemId로 해석되어 타입 변환 오류가 난다.
     */
    @GetMapping("/items/expiring")
    public ResponseEntity<ApiResponse<List<FridgeItemResponse>>> getExpiringItems(
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(fridgeService.getExpiringItems(userId))
        );
    }

    /**
     * 재료 등록
     * POST /api/fridge/items
     *
     * 요청 바디:
     * {
     *   "ingredientId": 3020,
     *   "quantity": 2,
     *   "unit": "개",
     *   "expiryDate": "2026-08-21"
     * }
     *
     * quantity / unit / expiryDate는 모두 선택 항목이다.
     * ingredientId는 재료 검색 API(GET /api/ingredients/search)로 먼저 확정해야 한다.
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<FridgeItemResponse>> addItem(
            Authentication authentication,
            @Valid @RequestBody FridgeItemRequest request
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(fridgeService.addItem(userId, request)));
    }

    /**
     * 재료 수정 (본인 것만)
     * PUT /api/fridge/items/{itemId}
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<FridgeItemResponse>> updateItem(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody FridgeItemRequest request
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(fridgeService.updateItem(userId, itemId, request))
        );
    }

    /**
     * 재료 삭제 (본인 것만)
     * DELETE /api/fridge/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            Authentication authentication,
            @PathVariable Long itemId
    ) {
        Long userId = requireUserId(authentication);

        fridgeService.deleteItem(userId, itemId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    // ─── 인증 처리 헬퍼 ────────────────────────────────────────

    private Long requireUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return userResolver.requireUserId(null);
        }
        return userResolver.requireUserId(authentication.getName());
    }
}
