package com.example.mealdangapi.review.controller;

import com.example.mealdangapi.board.service.AuthenticatedUserResolver;
import com.example.mealdangapi.global.common.ApiResponse;
import com.example.mealdangapi.global.common.PageResponse;
import com.example.mealdangapi.review.dto.request.ReviewRequest;
import com.example.mealdangapi.review.dto.response.ReviewResponse;
import com.example.mealdangapi.review.dto.response.ReviewSummaryResponse;
import com.example.mealdangapi.review.service.ReviewService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 후기 API — 담당: 종선
 *
 * ─── 경로 설계 ──────────────────────────────────────────────
 *   후기는 레시피에 속하므로 조회·작성은 /api/recipes/{recipeId}/reviews 아래에 둔다.
 *   수정·삭제는 이미 reviewId를 알고 있는 상태라 /api/reviews/{reviewId}로 뺀다.
 *   (레시피 경로를 붙이면 URL만 길어지고 얻는 게 없다)
 *
 * ─── 인증 정책 ──────────────────────────────────────────────
 *   목록·요약 조회 : 비로그인 허용
 *   작성·수정·삭제 : 로그인 필수
 *
 * ★ SecurityConfig에 GET /api/recipes/** 는 이미 permitAll로 설정되어 있어
 *   후기 조회는 별도 설정 없이 동작한다.
 *   다만 POST /api/recipes/{id}/reviews 는 인증이 필요한데,
 *   레시피 등록(POST /api/recipes)과 경로 패턴이 겹치지 않는지 확인이 필요하다.
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthenticatedUserResolver userResolver;

    /**
     * 후기 목록 조회 (비로그인 허용)
     * GET /api/recipes/{recipeId}/reviews?page=0&size=10
     */
    @GetMapping("/api/recipes/{recipeId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
            Authentication authentication,
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = resolveUserIdOrNull(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(reviewService.getReviews(userId, recipeId, page, size))
        );
    }

    /**
     * 후기 요약 조회 (비로그인 허용)
     * GET /api/recipes/{recipeId}/reviews/summary
     *
     * 레시피 상세 상단의 "후기 평점 4.6 / 조리 완료 27회" 영역용.
     */
    @GetMapping("/api/recipes/{recipeId}/reviews/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getSummary(
            Authentication authentication,
            @PathVariable Long recipeId
    ) {
        Long userId = resolveUserIdOrNull(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(reviewService.getSummary(userId, recipeId))
        );
    }

    /**
     * 후기 작성 (로그인 필수)
     * POST /api/recipes/{recipeId}/reviews
     *
     * 요청 바디: { "rating": 5, "content": "맛있어요" }
     * 회원당 레시피 1개만 작성 가능하다. 이미 썼으면 409.
     */
    @PostMapping("/api/recipes/{recipeId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            Authentication authentication,
            @PathVariable Long recipeId,
            @Valid @RequestBody ReviewRequest request
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        reviewService.createReview(userId, recipeId, request)
                ));
    }

    /**
     * 후기 수정 (로그인 필수, 본인만)
     * PUT /api/reviews/{reviewId}
     */
    @PutMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            Authentication authentication,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(reviewService.updateReview(userId, reviewId, request))
        );
    }

    /**
     * 후기 삭제 (로그인 필수, 본인만)
     * DELETE /api/reviews/{reviewId}
     */
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            Authentication authentication,
            @PathVariable Long reviewId
    ) {
        Long userId = requireUserId(authentication);

        reviewService.deleteReview(userId, reviewId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    // ─── 인증 처리 헬퍼 ────────────────────────────────────────
    // 게시판 컨트롤러와 동일한 방식이다.
    // AuthenticatedUserResolver가 board.service에 있는데, 도메인 두 곳에서 쓰게 됐으니
    // 정리한다면 global 쪽으로 옮기는 게 맞다. (지금은 동작에 문제없어 그대로 둔다)

    private Long requireUserId(Authentication authentication) {
        return userResolver.requireUserId(extractEmail(authentication));
    }

    private Long resolveUserIdOrNull(Authentication authentication) {
        return userResolver.resolveUserIdOrNull(extractEmail(authentication));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }
}
