package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends
        JpaRepository<Recipe, Long>,
        JpaSpecificationExecutor<Recipe> {

    Optional<Recipe> findByRecipeIdAndActiveTrue(Long recipeId);

    @Query("""
            SELECT r
            FROM Recipe r
            WHERE r.active = true
              AND r.chefCode = :chefCode
              AND r.annoyanceScore >= :minScore
              AND r.annoyanceScore < :maxScore
              AND EXISTS (
                  SELECT 1
                  FROM RecipeMeal rm
                  WHERE rm.recipe = r
                    AND rm.id.mealTime = :mealTime
              )
            ORDER BY r.recipeId ASC
            """)
    List<Recipe> findActiveCandidateRecipes(
            @Param("chefCode") ChefCode chefCode,
            @Param("minScore") BigDecimal minScore,
            @Param("maxScore") BigDecimal maxScore,
            @Param("mealTime") MealTime mealTime
    );
}
