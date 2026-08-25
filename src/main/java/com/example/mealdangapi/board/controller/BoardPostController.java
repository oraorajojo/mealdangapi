package com.example.mealdangapi.board.controller;

import com.example.mealdangapi.board.dto.request.PostReportRequest;
import com.example.mealdangapi.board.dto.response.BoardPostDetailResponse;
import com.example.mealdangapi.board.dto.response.BoardPostListItemResponse;
import com.example.mealdangapi.board.dto.response.PostLikeResponse;
import com.example.mealdangapi.board.dto.response.PostReportResponse;
import com.example.mealdangapi.board.service.AuthenticatedUserResolver;
import com.example.mealdangapi.board.service.BoardPostService;
import com.example.mealdangapi.board.service.PostLikeService;
import com.example.mealdangapi.board.service.PostReportService;
import com.example.mealdangapi.global.common.ApiResponse;
import com.example.mealdangapi.global.common.PageResponse;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시판(미식 연구소) API — 담당: 종선
 *
 * ─── 인증 정책 ──────────────────────────────────────────────
 *   목록·상세 조회 : 비로그인 허용
 *   좋아요·신고    : 로그인 필수
 *
 * ★ SecurityConfig에 GET /api/board/** permitAll 설정이 추가되어야
 *   비로그인 조회가 동작한다. (치연 담당 파일 — 요청 전달 완료)
 */
@RestController
@RequestMapping("/api/board/posts")
@RequiredArgsConstructor
public class BoardPostController {

    private final BoardPostService boardPostService;
    private final PostLikeService postLikeService;
    private final PostReportService postReportService;
    private final AuthenticatedUserResolver userResolver;

    /**
     * 게시판 목록 조회 (비로그인 허용)
     *
     * GET /api/board/posts?chefCode=KOREAN&mealTime=DINNER&page=0&size=12
     *
     * chefCode    : KOREAN / CHINESE / WESTERN / ETC   (생략 시 전체)
     * mealTime    : BREAKFAST / LUNCH / DINNER / LATE_NIGHT (생략 시 전체)
     * ingredients : "계란,김치"처럼 콤마로 구분한 재료명. 레시피에 그 재료가
     *               하나라도 들어간 게시글만 남긴다(생략 시 전체). 추천 결과
     *               화면의 "연계 추천"이 검색한 재료와 관련된 게시글만 보여주기 위한 것.
     *
     * 와이어프레임의 "전체 / 한식 / 중식 / 양식 / 기타" 및
     * "전체 / 아침 / 점심 / 저녁 / 야식" 필터에 대응한다.
     * "전체"를 선택하면 파라미터를 아예 보내지 않으면 된다.
     *
     * size 기본값 12는 3열 × 4행 카드 배치를 따랐다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BoardPostListItemResponse>>> getPosts(
            Authentication authentication,
            @RequestParam(required = false) ChefCode chefCode,
            @RequestParam(required = false) MealTime mealTime,
            @RequestParam(required = false) String ingredients,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Long userId = resolveUserIdOrNull(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        boardPostService.getPosts(userId, chefCode, mealTime, ingredients, page, size)
                )
        );
    }

    /**
     * 게시글 상세 조회 (비로그인 허용). 조회수가 1 증가한다.
     * GET /api/board/posts/{postId}
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<BoardPostDetailResponse>> getPostDetail(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        Long userId = resolveUserIdOrNull(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(boardPostService.getPostDetail(userId, postId))
        );
    }

    /**
     * 레시피 ID로 게시글 조회 (비로그인 허용)
     * GET /api/board/posts/by-recipe/{recipeId}
     *
     * 레시피 상세 화면의 신고 버튼에 넘길 postId를 얻는 용도.
     */
    @GetMapping("/by-recipe/{recipeId}")
    public ResponseEntity<ApiResponse<BoardPostDetailResponse>> getPostByRecipe(
            Authentication authentication,
            @PathVariable Long recipeId
    ) {
        Long userId = resolveUserIdOrNull(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(boardPostService.getPostByRecipeId(userId, recipeId))
        );
    }

    /**
     * 좋아요 등록 (로그인 필수)
     * POST /api/board/posts/{postId}/likes
     */
    @PostMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeResponse>> like(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(postLikeService.like(userId, postId))
        );
    }

    /**
     * 좋아요 취소 (로그인 필수)
     * DELETE /api/board/posts/{postId}/likes
     */
    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<PostLikeResponse>> unlike(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity.ok(
                ApiResponse.ok(postLikeService.unlike(userId, postId))
        );
    }

    /**
     * 신고 접수 (로그인 필수)
     * POST /api/board/posts/{postId}/reports
     *
     * 요청 바디: { "reasonCode": "SPAM", "etcReason": null }
     * reasonCode가 ETC이면 etcReason이 필수다.
     */
    @PostMapping("/{postId}/reports")
    public ResponseEntity<ApiResponse<PostReportResponse>> report(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody PostReportRequest request
    ) {
        Long userId = requireUserId(authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        postReportService.report(userId, postId, request)
                ));
    }

    /**
     * 게시글 삭제 (로그인 필수, 본인 글 또는 관리자)
     * DELETE /api/board/posts/{postId}
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        Long userId = requireUserId(authentication);
        boardPostService.deletePost(userId, postId);

        return ResponseEntity.noContent().build();
    }

    // ─── 인증 처리 헬퍼 ────────────────────────────────────────

    /** 로그인 필수 API용. 비로그인이면 401 */
    private Long requireUserId(Authentication authentication) {
        return userResolver.requireUserId(extractEmail(authentication));
    }

    /** 비로그인 허용 API용. 로그인 상태면 userId, 아니면 null */
    private Long resolveUserIdOrNull(Authentication authentication) {
        return userResolver.resolveUserIdOrNull(extractEmail(authentication));
    }

    /**
     * Authentication에서 이메일(username)을 꺼낸다.
     *
     * 비로그인이면 authentication이 null이거나, Spring Security가
     * 익명 사용자를 "anonymousUser"라는 이름으로 채워 넣는다.
     * 둘 다 미로그인 상태이므로 null로 통일한다.
     */
    private String extractEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }
}
