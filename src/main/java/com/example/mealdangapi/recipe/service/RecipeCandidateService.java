package com.example.mealdangapi.recipe.service;

import com.example.mealdangapi.recipe.dto.RecipeCandidateIngredientResponse;
import com.example.mealdangapi.recipe.dto.RecipeCandidateResponse;
import com.example.mealdangapi.recipe.entity.AnnoyanceBand;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.repository.RecipeIngredientRepository;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true )
public class RecipeCandidateService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;

    public List<RecipeCandidateResponse> findCandidateRecipes(
            ChefCode chefCode,
            AnnoyanceBand annoyanceBand,
            MealTime mealTime
    ) {
        validateRequest(
                chefCode,
                annoyanceBand,
                mealTime
        );

        List<Recipe> recipes = recipeRepository.findActiveCandidateRecipes(
                chefCode,
                annoyanceBand.getMinInclusive(),
                annoyanceBand.getMaxExclusive(),
                mealTime
        );

        if (recipes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> recipeIds = recipes
                .stream()
                .map(Recipe::getRecipeId)
                .toList();

        Map<Long, List<RecipeCandidateIngredientResponse>> ingredientsByRecipeId =
                recipeIngredientRepository
                        .findAllWithIngredientByRecipeIds(recipeIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                recipeIngredient -> recipeIngredient
                                        .getRecipe()
                                        .getRecipeId(),
                                Collectors.mapping(
                                        RecipeCandidateIngredientResponse::from,
                                        Collectors.toList()
                                )
                        ));

        return recipes
                .stream()
                .map(recipe -> RecipeCandidateResponse.of(
                        recipe,
                        ingredientsByRecipeId.getOrDefault(
                                recipe.getRecipeId(),
                                Collections.emptyList()
                        )
                ))
                .toList();
    }

    private void validateRequest(
            ChefCode chefCode,
            AnnoyanceBand annoyanceBand,
            MealTime mealTime
    ) {
        if (chefCode == null
                || annoyanceBand == null
                || mealTime == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "chefCode, annoyanceBand, mealTime은 모두 필수입니다."
            );
        }

        if (chefCode == ChefCode.ETC) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ETC 셰프는 일반 추천 후보 조회 대상이 아닙니다."
            );
        }
    }
}
