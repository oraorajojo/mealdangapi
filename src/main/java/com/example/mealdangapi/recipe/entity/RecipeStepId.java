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
public class RecipeStepId implements Serializable {

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(
            name = "step_no",
            columnDefinition = "SMALLINT UNSIGNED"
    )
    private Integer stepNo;

    private RecipeStepId(Long recipeId, Integer stepNo) {
        this.recipeId = recipeId;
        this.stepNo = stepNo;
    }

    public static RecipeStepId of(
            Long recipeId,
            Integer stepNo
    ) {
        return new RecipeStepId(recipeId, stepNo);
    }
}
