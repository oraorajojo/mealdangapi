package com.example.mealdangapi.recommend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChefSelectionRepository extends JpaRepository<ChefSelection, Long> {
    boolean existsByRecommendLogId(Long recommendLogId);

    /** 레시피 상세의 "조리 완료" 집계용. 후기 수와 합산해서 쓴다 */
    long countByRecipeId(Long recipeId);
}
