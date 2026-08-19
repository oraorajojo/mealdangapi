package com.example.mealdangapi.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** FastAPI -> Spring 응답 (POST /recommend) */
public record FastApiRecommendResponse(
    @JsonProperty("parsed_ingredients") List<String> parsedIngredients,
    @JsonProperty("normalized_ingredient_ids") List<Long> normalizedIngredientIds,
    @JsonProperty("portion_hint") String portionHint,
    Map<String, FastApiRecipeResult> results
) {
}
