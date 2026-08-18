package com.example.mealdangapi.recipe.dto;

import com.example.mealdangapi.recipe.entity.RecipeIngredient;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RecipeIngredientResponse {

    private Long ingredientId;
    private String name;
    private BigDecimal amountValue;
    private String amountUnit;
    private boolean essential;
    private Short sortOrder;

    public static RecipeIngredientResponse from(
            RecipeIngredient recipeIngredient
    ) {
        return new RecipeIngredientResponse(
                recipeIngredient.getIngredient().getIngredientId(),
                recipeIngredient.getIngredient().getName(),
                recipeIngredient.getAmountValue(),
                recipeIngredient.getAmountUnit(),
                recipeIngredient.isEssential(),
                recipeIngredient.getSortOrder()
        );
    }
}
