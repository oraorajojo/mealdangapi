package com.example.mealdangapi.recipe.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecipeStepCreateRequest {

    private String content;
    private String imageUrl;
}
