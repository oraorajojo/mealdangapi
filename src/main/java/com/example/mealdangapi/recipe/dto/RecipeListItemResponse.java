package com.example.mealdangapi.recipe.dto;

import com.example.mealdangapi.recipe.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class RecipeListItemResponse {

    private Long recipeId;
    private String chefCode;
    private String name;
    private String summary;
    private Integer cookingTimeMin;
    private Integer baseServings;
    private BigDecimal annoyanceScore;
    private Integer knifeLevel;
    private Integer dishCount;
    private String imageUrl;
    private List<String> mealTimes;

    public static RecipeListItemResponse of(
            Recipe recipe,
            List<String> mealTimes
    ) {
        return new RecipeListItemResponse(
                recipe.getRecipeId(),
                recipe.getChefCode().name(),
                recipe.getName(),
                recipe.getSummary(),
                recipe.getCookingTimeMin(),
                recipe.getBaseServings(),
                recipe.getAnnoyanceScore(),
                recipe.getKnifeLevel(),
                recipe.getDishCount(),
                recipe.getImageUrl(),
                mealTimes
        );
    }
}
