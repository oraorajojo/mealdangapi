package com.example.mealdangapi.recipe.dto;

import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class RecipeDetailResponse {

    private Long recipeId;
    private Long postId;
    private String chefCode;
    private String name;
    private String summary;
    private Integer cookingTimeMin;
    private Integer baseServings;
    private BigDecimal annoyanceScore;
    private Integer knifeLevel;
    private Integer dishCount;
    private String imageUrl;
    private String sourceType;
    private List<MealTime> mealTimes;
    private List<RecipeIngredientResponse> ingredients;
    private List<RecipeStepResponse> steps;

    /** recipe_bookmarks 테이블을 COUNT한 실시간 찜 수 */
    private long bookmarkCount;

    public static RecipeDetailResponse of(
            Recipe recipe,
            Long postId,
            List<MealTime> mealTimes,
            List<RecipeIngredientResponse> ingredients,
            List<RecipeStepResponse> steps,
            long bookmarkCount
    ) {
        return new RecipeDetailResponse(
                recipe.getRecipeId(),
                postId,
                recipe.getChefCode().name(),
                recipe.getName(),
                recipe.getSummary(),
                recipe.getCookingTimeMin(),
                recipe.getBaseServings(),
                recipe.getAnnoyanceScore(),
                recipe.getKnifeLevel(),
                recipe.getDishCount(),
                recipe.getImageUrl(),
                recipe.getSourceType().name(),
                mealTimes,
                ingredients,
                steps,
                bookmarkCount
        );
    }
}
