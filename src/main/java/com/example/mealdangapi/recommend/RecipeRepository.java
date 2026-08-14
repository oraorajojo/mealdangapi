package com.example.mealdangapi.recommend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // 식사 시간대에 맞고 활성화된 레시피만 후보로 조회 (ETC 장르는 추천 대상에서 제외)
    @Query(value = """
        SELECT r.recipe_id AS recipeId, r.chef_code AS chefCode, r.name AS name,
               r.cooking_time_min AS cookingTimeMin, r.annoyance_score AS annoyanceScore,
               r.knife_level AS knifeLevel, r.dish_count AS dishCount, r.image_url AS imageUrl
        FROM recipes r
        JOIN recipe_meals rm ON rm.recipe_id = r.recipe_id
        WHERE r.is_active = TRUE AND rm.meal_time = :mealTime AND r.chef_code <> 'ETC'
        """, nativeQuery = true)
    List<RecipeCandidateRow> findActiveCandidates(@Param("mealTime") String mealTime);

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
