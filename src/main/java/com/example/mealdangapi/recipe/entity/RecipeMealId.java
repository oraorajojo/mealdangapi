package com.example.mealdangapi.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeMealId implements Serializable {

    @Column(name = "recipe_id")
    private Long recipeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_time")
    private MealTime mealTime;

    private RecipeMealId(Long recipeId, MealTime mealTime) {
        this.recipeId = recipeId;
        this.mealTime = mealTime;
    }

    public static RecipeMealId of(
            Long recipeId,
            MealTime mealTime
    ) {
        return new RecipeMealId(recipeId, mealTime);
    }
}
