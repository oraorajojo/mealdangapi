package com.example.mealdangapi.recipe.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class RecipeIngredientCreateRequest {

    private Long ingredientId;
    private BigDecimal amountValue;
    private String amountUnit;
    private boolean essential = true;
}
