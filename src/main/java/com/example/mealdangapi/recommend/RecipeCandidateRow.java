package com.example.mealdangapi.recommend;

import java.math.BigDecimal;

/** recipeRepository.findActiveCandidates() 네이티브 쿼리 결과를 받는 프로젝션. */
public interface RecipeCandidateRow {
    Long getRecipeId();
    String getChefCode();
    String getName();
    Integer getCookingTimeMin();
    BigDecimal getAnnoyanceScore();
    Integer getKnifeLevel();
    Integer getDishCount();
    String getImageUrl();
}
