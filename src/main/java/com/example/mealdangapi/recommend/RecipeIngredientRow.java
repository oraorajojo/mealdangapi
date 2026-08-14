package com.example.mealdangapi.recommend;

/** recipeRepository.findIngredientNames() 네이티브 쿼리 결과를 받는 프로젝션. */
public interface RecipeIngredientRow {
    Long getRecipeId();
    String getIngredientName();
}
