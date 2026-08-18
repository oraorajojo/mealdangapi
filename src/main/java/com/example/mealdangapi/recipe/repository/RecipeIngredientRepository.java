package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.RecipeIngredient;
import com.example.mealdangapi.recipe.entity.RecipeIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, RecipeIngredientId> {

    @Query("""
            SELECT ri
            FROM RecipeIngredient ri
            JOIN FETCH ri.ingredient
            WHERE ri.recipe.recipeId = :recipeId
            ORDER BY ri.sortOrder ASC
            """)
    List<RecipeIngredient> findAllWithIngredientByRecipeId(
            @Param("recipeId") Long recipeId
    );

    @Query("""
            SELECT ri
            FROM RecipeIngredient ri
            JOIN FETCH ri.ingredient
            WHERE ri.recipe.recipeId IN :recipeIds
            ORDER BY ri.recipe.recipeId ASC, ri.sortOrder ASC
            """)
    List<RecipeIngredient> findAllWithIngredientByRecipeIds(
            @Param("recipeIds") Collection<Long> recipeIds
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM RecipeIngredient ri
            WHERE ri.recipe.recipeId = :recipeId
            """)
    void deleteAllByRecipeId(
            @Param("recipeId") Long recipeId
    );
}
