package com.example.mealdangapi.recommend.dto;

import java.util.List;
import java.util.Map;

/** React -> Spring 요청 (POST /api/recommend) */
public record RecommendRequest(
    String ingredientsText,
    String mealTime,
    List<String> conditions,
    List<String> excludeIngredients,
    Map<String, List<Long>> excludeRecipeIds
) {
}
