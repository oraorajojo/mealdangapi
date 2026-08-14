package com.example.mealdangapi.recommend.dto;

import java.util.List;
import java.util.Map;

/** Spring -> React 응답 (POST /api/recommend) */
public record RecommendResponse(
    Long recommendLogId,
    List<String> parsedIngredients,
    String portionHint,
    Map<String, RecipeResultDto> results
) {
}
