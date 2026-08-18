package com.example.mealdangapi.recipe.entity;

import com.example.mealdangapi.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long recipeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chef_code", nullable = false)
    private ChefCode chefCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User author;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(
            name = "cooking_time_min",
            nullable = false,
            columnDefinition = "SMALLINT UNSIGNED"
    )
    private Integer cookingTimeMin;

    @Column(
            name = "base_servings",
            nullable = false,
            columnDefinition = "SMALLINT UNSIGNED"
    )
    private Integer baseServings;

    @Column(name = "annoyance_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal annoyanceScore;

    @Column(
            name = "knife_level",
            nullable = false,
            columnDefinition = "TINYINT UNSIGNED"
    )
    private Integer knifeLevel;

    @Column(
            name = "dish_count",
            nullable = false,
            columnDefinition = "TINYINT UNSIGNED"
    )
    private Integer dishCount;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private RecipeSourceType sourceType;

    @Column(name = "source_recipe_key", length = 100)
    private String sourceRecipeKey;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Recipe createUserRecipe(
            User author,
            ChefCode chefCode,
            String name,
            String summary,
            Integer cookingTimeMin,
            Integer baseServings,
            BigDecimal annoyanceScore,
            Integer knifeLevel,
            Integer dishCount,
            String imageUrl
    ) {
        Recipe recipe = new Recipe();
        recipe.author = author;
        recipe.chefCode = chefCode;
        recipe.name = name;
        recipe.summary = summary;
        recipe.cookingTimeMin = cookingTimeMin;
        recipe.baseServings = baseServings;
        recipe.annoyanceScore = annoyanceScore;
        recipe.knifeLevel = knifeLevel;
        recipe.dishCount = dishCount;
        recipe.imageUrl = imageUrl;
        recipe.sourceType = RecipeSourceType.USER_SUBMISSION;
        recipe.sourceRecipeKey = null;
        recipe.active = true;
        return recipe;
    }

    public void updateUserRecipe(
            ChefCode chefCode,
            String name,
            String summary,
            Integer cookingTimeMin,
            Integer baseServings,
            BigDecimal annoyanceScore,
            Integer knifeLevel,
            Integer dishCount,
            String imageUrl
    ) {
        this.chefCode = chefCode;
        this.name = name;
        this.summary = summary;
        this.cookingTimeMin = cookingTimeMin;
        this.baseServings = baseServings;
        this.annoyanceScore = annoyanceScore;
        this.knifeLevel = knifeLevel;
        this.dishCount = dishCount;
        this.imageUrl = imageUrl;
    }

    public void deactivate() {
        this.active = false;
    }
}
