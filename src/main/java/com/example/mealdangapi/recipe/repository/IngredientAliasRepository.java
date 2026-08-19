package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.IngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, Long> {

    @Query("SELECT ia FROM IngredientAlias ia JOIN FETCH ia.ingredient")
    List<IngredientAlias> findAllWithIngredient();

    @Query("SELECT ia FROM IngredientAlias ia JOIN FETCH ia.ingredient WHERE ia.alias LIKE %:keyword%")
    List<IngredientAlias> searchByAlias(@Param("keyword") String keyword);
}
