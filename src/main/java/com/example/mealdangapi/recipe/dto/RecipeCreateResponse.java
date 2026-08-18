package com.example.mealdangapi.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecipeCreateResponse {

    private Long recipeId;
    private String message;
}
