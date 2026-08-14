package com.example.mealdangapi.recommend.dto;

import java.math.BigDecimal;
import java.util.List;

/** Spring -> React 응답에 담기는 셰프 1명분 추천 결과 */
public record RecipeResultDto(
    Long recipeId,
    String name,
    Integer cookingTimeMin,
    BigDecimal annoyanceScore,
    Integer knifeLevel,
    Integer dishCount,
    String imageUrl,
    List<String> ingredients,
    String message,
    String substituteTip,
    BigDecimal matchScore
) {
}
