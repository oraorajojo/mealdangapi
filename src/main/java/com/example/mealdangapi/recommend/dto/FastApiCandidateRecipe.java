package com.example.mealdangapi.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/** Spring -> FastAPI 후보 레시피 1건. FastAPI 쪽 필드명(snake_case)에 맞춘다. */
public record FastApiCandidateRecipe(
    @JsonProperty("recipe_id") Long recipeId,
    @JsonProperty("chef_code") String chefCode,
    String name,
    @JsonProperty("cooking_time_min") Integer cookingTimeMin,
    @JsonProperty("annoyance_score") BigDecimal annoyanceScore,
    @JsonProperty("knife_level") Integer knifeLevel,
    @JsonProperty("dish_count") Integer dishCount,
    @JsonProperty("image_url") String imageUrl,
    List<String> ingredients
) {
}
