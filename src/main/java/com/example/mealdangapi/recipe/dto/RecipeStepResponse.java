package com.example.mealdangapi.recipe.dto;

import com.example.mealdangapi.recipe.entity.RecipeStep;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecipeStepResponse {

    private Integer stepNo;
    private String content;
    private String imageUrl;

    public static RecipeStepResponse from(RecipeStep recipeStep) {
        return new RecipeStepResponse(
                recipeStep.getId().getStepNo(),
                recipeStep.getContent(),
                recipeStep.getImageUrl()
        );
    }
}
