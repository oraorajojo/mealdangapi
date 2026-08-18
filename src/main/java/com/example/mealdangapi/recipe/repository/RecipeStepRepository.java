package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.RecipeStep;
import com.example.mealdangapi.recipe.entity.RecipeStepId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeStepRepository extends JpaRepository<RecipeStep, RecipeStepId> {

    @Query("""
            SELECT rs
            FROM RecipeStep rs
            WHERE rs.recipe.recipeId = :recipeId
            ORDER BY rs.id.stepNo ASC
            """)
    List<RecipeStep> findAllByRecipeIdOrderByStepNo(
            @Param("recipeId") Long recipeId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM RecipeStep rs
            WHERE rs.recipe.recipeId = :recipeId
            """)
    void deleteAllByRecipeId(
            @Param("recipeId") Long recipeId
    );
}
