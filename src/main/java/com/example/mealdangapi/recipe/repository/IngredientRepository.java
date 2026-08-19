package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findByNameContainingOrderByName(String keyword);
}
