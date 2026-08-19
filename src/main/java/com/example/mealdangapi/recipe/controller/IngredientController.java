package com.example.mealdangapi.recipe.controller;

import com.example.mealdangapi.recipe.dto.IngredientSearchResponse;
import com.example.mealdangapi.recipe.service.IngredientSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientSearchService ingredientSearchService;

    /**
     * 재료 자동완성 검색 (비로그인 허용) — 레시피 재료 입력·냉장고 재료 입력 공통 사용
     * GET /api/ingredients/search?keyword=계란
     *
     * 표준 재료명·별칭 양쪽에서 검색하며, 항상 표준 재료(ingredientId + 표준명)로 응답한다.
     * 매칭이 없으면 빈 배열을 반환한다 (404 아님).
     */
    @GetMapping("/search")
    public ResponseEntity<List<IngredientSearchResponse>> search(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(ingredientSearchService.search(keyword));
    }
}
