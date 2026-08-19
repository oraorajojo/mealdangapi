package com.example.mealdangapi.recommend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendLogResultRepository extends JpaRepository<RecommendLogResult, RecommendLogResultId> {
    boolean existsByRecommendLogIdAndRecipeId(Long recommendLogId, Long recipeId);
}
