package com.example.mealdangapi.recipe.storage;

import com.example.mealdangapi.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me" )
@RequiredArgsConstructor
public class RecipeStorageController {

    private final RecipeStorageService recipeStorageService;

    /**
     * 찜 등록. JWT 인증 사용자만 자신의 찜을 등록할 수 있다.
     */
    @PostMapping("/bookmarks/{recipeId}")
    public ResponseEntity<ApiResponse<Void>> addBookmark(
            Authentication authentication,
            @PathVariable Long recipeId
    ) {
        recipeStorageService.addBookmark(authentication.getName(), recipeId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }

    /**
     * 찜 해제. 본인의 찜 레코드만 삭제한다.
     */
    @DeleteMapping("/bookmarks/{recipeId}")
    public ResponseEntity<Void> removeBookmark(
            Authentication authentication,
            @PathVariable Long recipeId
    ) {
        recipeStorageService.removeBookmark(authentication.getName(), recipeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 내가 찜한 활성 레시피 목록 조회.
     */
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<List<RecipeStorageService.StoredRecipeResponse>>>
    getBookmarks(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        recipeStorageService.getBookmarks(
                                authentication.getName(),
                                size
                        )
                )
        );
    }

    /**
     * 레시피 상세 화면 진입 시 호출하는 최근 본 메뉴 기록 upsert.
     * 같은 레시피를 다시 열면 행을 중복 생성하지 않고 last_viewed_at만 갱신한다.
     */
    @PostMapping("/recent-views/{recipeId}")
    public ResponseEntity<ApiResponse<Void>> recordRecentView(
            Authentication authentication,
            @PathVariable Long recipeId
    ) {
        recipeStorageService.recordRecentView(authentication.getName(), recipeId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 내가 최근 본 활성 레시피 목록 조회.
     */
    @GetMapping("/recent-views")
    public ResponseEntity<ApiResponse<List<RecipeStorageService.StoredRecipeResponse>>>
    getRecentViews(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        recipeStorageService.getRecentViews(
                                authentication.getName(),
                                size
                        )
                )
        );
    }
}
