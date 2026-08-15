package com.example.mealdangapi.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredientId implements Serializable {

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "ingredient_id")
    private Long ingredientId;

    private RecipeIngredientId(Long recipeId, Long ingredientId) {
        this.recipeId = recipeId;
        this.ingredientId = ingredientId;
    }

    public static RecipeIngredientId of(
            Long recipeId,
            Long ingredientId
    ) {
        return new RecipeIngredientId(recipeId, ingredientId);
    }
}
