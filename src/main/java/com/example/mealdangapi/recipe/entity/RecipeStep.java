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

@Entity
@Table(name = "recipe_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeStep {

    @EmbeddedId
    private RecipeStepId id;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    public static RecipeStep create(
            Recipe recipe,
            int stepNo,
            String content,
            String imageUrl
    ) {
        RecipeStep recipeStep = new RecipeStep();
        recipeStep.id = RecipeStepId.of(
                recipe.getRecipeId(),
                stepNo
        );
        recipeStep.recipe = recipe;
        recipeStep.content = content;
        recipeStep.imageUrl = imageUrl;
        return recipeStep;
    }
}
