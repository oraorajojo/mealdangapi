package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.IngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, Long> {

    @Query("SELECT ia FROM IngredientAlias ia JOIN FETCH ia.ingredient")
    List<IngredientAlias> findAllWithIngredient();
}
