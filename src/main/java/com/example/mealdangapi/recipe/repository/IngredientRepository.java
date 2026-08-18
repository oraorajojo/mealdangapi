package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
