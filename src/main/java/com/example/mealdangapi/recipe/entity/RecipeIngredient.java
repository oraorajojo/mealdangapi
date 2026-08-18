package com.example.mealdangapi.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredient {

    @EmbeddedId
    private RecipeIngredientId id;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @MapsId("ingredientId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "amount_value", precision = 10, scale = 2)
    private BigDecimal amountValue;

    @Column(name = "amount_unit", length = 30)
    private String amountUnit;

    @Column(name = "is_essential", nullable = false)
    private boolean essential;

    @Column(
            name = "sort_order",
            nullable = false,
            columnDefinition = "SMALLINT UNSIGNED"
    )
    private Integer sortOrder;

    public static RecipeIngredient create(
            Recipe recipe,
            Ingredient ingredient,
            BigDecimal amountValue,
            String amountUnit,
            boolean essential,
            int sortOrder
    ) {
        RecipeIngredient recipeIngredient = new RecipeIngredient();
        recipeIngredient.id = RecipeIngredientId.of(
                recipe.getRecipeId(),
                ingredient.getIngredientId()
        );
        recipeIngredient.recipe = recipe;
        recipeIngredient.ingredient = ingredient;
        recipeIngredient.amountValue = amountValue;
        recipeIngredient.amountUnit = amountUnit;
        recipeIngredient.essential = essential;
        recipeIngredient.sortOrder = sortOrder;
        return recipeIngredient;
    }
}
