package com.example.mealdangapi.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/** FastAPI -> Spring 응답 안에 담긴 셰프 1명분 결과 (snake_case 그대로 매핑) */
public record FastApiRecipeResult(
    @JsonProperty("recipe_id") Long recipeId,
    String name,
    @JsonProperty("cooking_time_min") Integer cookingTimeMin,
    @JsonProperty("annoyance_score") BigDecimal annoyanceScore,
    @JsonProperty("knife_level") Integer knifeLevel,
    @JsonProperty("dish_count") Integer dishCount,
    @JsonProperty("image_url") String imageUrl,
    List<String> ingredients,
    String message,
    @JsonProperty("substitute_tip") String substituteTip,
    @JsonProperty("match_score") BigDecimal matchScore
) {
}
