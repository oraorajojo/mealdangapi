package com.example.mealdangapi.recipe.dto;

/** 재료 검색/자동완성 결과 1건. 표준 재료명과 ID만 내려준다. */
public record IngredientSearchResponse(
        Long ingredientId,
        String name
) {
}
