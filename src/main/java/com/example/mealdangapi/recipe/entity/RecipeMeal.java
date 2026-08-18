package com.example.mealdangapi.recipe.entity;

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

@Entity
@Table(name = "recipe_meals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeMeal {

    @EmbeddedId
    private RecipeMealId id;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    public static RecipeMeal create(
            Recipe recipe,
            MealTime mealTime
    ) {
        RecipeMeal recipeMeal = new RecipeMeal();
        recipeMeal.id = RecipeMealId.of(
                recipe.getRecipeId(),
                mealTime
        );
        recipeMeal.recipe = recipe;
        return recipeMeal;
    }
}
