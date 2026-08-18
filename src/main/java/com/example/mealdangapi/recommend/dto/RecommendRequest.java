package com.example.mealdangapi.recommend.dto;

import java.util.List;
import java.util.Map;

/** React -> Spring 요청 (POST /api/recommend)
 *  mealTime, annoyanceBand는 둘 다 선택값 — 미세조정을 안 열면 null로 와서 전체 대상으로 검색된다. */
public record RecommendRequest(
    String ingredientsText,
    String mealTime,
    String annoyanceBand,
    List<String> conditions,
    List<String> excludeIngredients,
    Map<String, List<Long>> excludeRecipeIds
) {
}
