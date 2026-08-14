package com.example.mealdangapi.recommend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // 활성화된 레시피 중 ETC 장르 제외. mealTime / minScore / maxScore는 전부 선택값으로,
    // null이면 해당 조건은 걸지 않는다 (미세조정 없이도 전체 대상으로 검색되도록).
    // recipe_meals는 레시피당 여러 행일 수 있어 JOIN 대신 EXISTS로 걸러 중복 후보가 안 생기게 한다.
    @Query(value = """
        SELECT r.recipe_id AS recipeId, r.chef_code AS chefCode, r.name AS name,
               r.cooking_time_min AS cookingTimeMin, r.annoyance_score AS annoyanceScore,
               r.knife_level AS knifeLevel, r.dish_count AS dishCount, r.image_url AS imageUrl
        FROM recipes r
        WHERE r.is_active = TRUE
          AND r.chef_code <> 'ETC'
          AND (:mealTime IS NULL OR EXISTS (
                SELECT 1 FROM recipe_meals rm
                WHERE rm.recipe_id = r.recipe_id AND rm.meal_time = :mealTime
              ))
          AND (:minScore IS NULL OR r.annoyance_score >= :minScore)
          AND (:maxScore IS NULL OR r.annoyance_score <= :maxScore)
        """, nativeQuery = true)
    List<RecipeCandidateRow> findActiveCandidates(
        @Param("mealTime") String mealTime,
        @Param("minScore") BigDecimal minScore,
        @Param("maxScore") BigDecimal maxScore
    );

    // 후보 레시피들의 재료명 목록을 한 번에 조회 (recipe_ingredients + ingredients 조인)
    @Query(value = """
        SELECT ri.recipe_id AS recipeId, i.name AS ingredientName
        FROM recipe_ingredients ri
        JOIN ingredients i ON i.ingredient_id = ri.ingredient_id
        WHERE ri.recipe_id IN :recipeIds
        ORDER BY ri.recipe_id, ri.sort_order
        """, nativeQuery = true)
    List<RecipeIngredientRow> findIngredientNames(@Param("recipeIds") List<Long> recipeIds);
}
