package com.example.mealdangapi.recipe.dto;

import com.example.mealdangapi.recipe.entity.RecipeIngredient;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RecipeCandidateIngredientResponse {

    private Long ingredientId;
    private String name;
    private BigDecimal amountValue;
    private String amountUnit;
    private boolean essential;

    public static RecipeCandidateIngredientResponse from(
            RecipeIngredient recipeIngredient
    ) {
        return new RecipeCandidateIngredientResponse(
                recipeIngredient.getIngredient().getIngredientId(),
                recipeIngredient.getIngredient().getName(),
                recipeIngredient.getAmountValue(),
                recipeIngredient.getAmountUnit(),
                recipeIngredient.isEssential()
        );
    }
}
