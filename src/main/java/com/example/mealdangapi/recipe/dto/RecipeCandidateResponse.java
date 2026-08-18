package com.example.mealdangapi.recipe.dto;

import com.example.mealdangapi.recipe.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class RecipeCandidateResponse {

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
    private List<RecipeCandidateIngredientResponse> ingredients;

    public static RecipeCandidateResponse of(
            Recipe recipe,
            List<RecipeCandidateIngredientResponse> ingredients
    ) {
        return new RecipeCandidateResponse(
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
                ingredients
        );
    }
}
