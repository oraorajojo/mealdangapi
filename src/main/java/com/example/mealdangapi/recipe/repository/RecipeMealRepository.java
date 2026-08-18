package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.RecipeMeal;
import com.example.mealdangapi.recipe.entity.RecipeMealId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RecipeMealRepository extends JpaRepository<RecipeMeal, RecipeMealId> {

    @Query("""
            SELECT rm.id.mealTime
            FROM RecipeMeal rm
            WHERE rm.recipe.recipeId = :recipeId
            ORDER BY rm.id.mealTime ASC
            """)
    List<MealTime> findMealTimesByRecipeId(
            @Param("recipeId") Long recipeId
    );

    @Query("""
            SELECT rm
            FROM RecipeMeal rm
            WHERE rm.recipe.recipeId IN :recipeIds
            ORDER BY rm.recipe.recipeId ASC, rm.id.mealTime ASC
            """)
    List<RecipeMeal> findAllByRecipeIds(
            @Param("recipeIds") Collection<Long> recipeIds
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM RecipeMeal rm
            WHERE rm.recipe.recipeId = :recipeId
            """)
    void deleteAllByRecipeId(
            @Param("recipeId") Long recipeId
    );
}
