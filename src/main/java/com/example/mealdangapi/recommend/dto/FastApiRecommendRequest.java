package com.example.mealdangapi.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Spring -> FastAPI 요청 (POST /recommend) */
public record FastApiRecommendRequest(
    @JsonProperty("raw_ingredients_text") String rawIngredientsText,
    @JsonProperty("meal_time") String mealTime,
    List<String> conditions,
    @JsonProperty("exclude_ingredients") List<String> excludeIngredients,
    @JsonProperty("exclude_recipe_ids") Map<String, List<Long>> excludeRecipeIds,
    @JsonProperty("candidate_recipes") List<FastApiCandidateRecipe> candidateRecipes,
    @JsonProperty("ingredient_dictionary") List<FastApiIngredientDictionaryEntry> ingredientDictionary
) {
}
