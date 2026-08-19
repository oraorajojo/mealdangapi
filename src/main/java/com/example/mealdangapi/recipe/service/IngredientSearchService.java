package com.example.mealdangapi.recipe.service;

import com.example.mealdangapi.recipe.dto.IngredientSearchResponse;
import com.example.mealdangapi.recipe.entity.Ingredient;
import com.example.mealdangapi.recipe.entity.IngredientAlias;
import com.example.mealdangapi.recipe.repository.IngredientAliasRepository;
import com.example.mealdangapi.recipe.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 재료 자동완성 검색 (레시피 재료 입력·냉장고 재료 입력 공통 사용) — 협의자료 §7
 *
 * 표준 재료명과 별칭 양쪽에서 검색하되, 결과는 항상 표준 재료(ingredientId + 표준명)로
 * 돌려준다. "달걀"로 검색해도 별칭 테이블을 거쳐 표준명 "계란"의 ingredientId가 나온다.
 */
@Service
@RequiredArgsConstructor
public class IngredientSearchService {

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository ingredientAliasRepository;

    @Transactional(readOnly = true)
    public List<IngredientSearchResponse> search(String keyword) {
        // 표준명 → 별칭 순으로 채워서, 표준명으로 이미 걸린 재료는 별칭 매칭으로 중복 추가되지 않게 한다.
        Map<Long, String> nameById = new LinkedHashMap<>();

        for (Ingredient ingredient : ingredientRepository.findByNameContainingOrderByName(keyword)) {
            nameById.put(ingredient.getIngredientId(), ingredient.getName());
        }

        for (IngredientAlias alias : ingredientAliasRepository.searchByAlias(keyword)) {
            Ingredient ingredient = alias.getIngredient();
            nameById.putIfAbsent(ingredient.getIngredientId(), ingredient.getName());
        }

        return nameById.entrySet().stream()
                .map(entry -> new IngredientSearchResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
}
