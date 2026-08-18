package com.example.mealdangapi.recipe.controller;

import com.example.mealdangapi.recipe.dto.RecipeCandidateResponse;
import com.example.mealdangapi.recipe.dto.RecipeCreateRequest;
import com.example.mealdangapi.recipe.dto.RecipeCreateResponse;
import com.example.mealdangapi.recipe.dto.RecipeDetailResponse;
import com.example.mealdangapi.recipe.dto.RecipeListResponse;
import com.example.mealdangapi.recipe.entity.AnnoyanceBand;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.service.RecipeCandidateService;
import com.example.mealdangapi.recipe.service.RecipeCommandService;
import com.example.mealdangapi.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/recipes" )
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeCommandService recipeCommandService;
    private final RecipeCandidateService recipeCandidateService;

    @GetMapping
    public ResponseEntity<RecipeListResponse> getRecipes(
            @RequestParam(required = false) ChefCode chefCode,
            @RequestParam(required = false) MealTime mealTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minAnnoyance,
            @RequestParam(required = false) BigDecimal maxAnnoyance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                recipeService.getRecipes(
                        chefCode,
                        mealTime,
                        keyword,
                        minAnnoyance,
                        maxAnnoyance,
                        page,
                        size
                )
        );
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<RecipeCandidateResponse>> getCandidateRecipes(
            @RequestParam ChefCode chefCode,
            @RequestParam AnnoyanceBand annoyanceBand,
            @RequestParam MealTime mealTime
    ) {
        return ResponseEntity.ok(
                recipeCandidateService.findCandidateRecipes(
                        chefCode,
                        annoyanceBand,
                        mealTime
                )
        );
    }

    @GetMapping("/{recipeId:\\d+}")
    public ResponseEntity<RecipeDetailResponse> getRecipeDetail(
            @PathVariable Long recipeId
    ) {
        return ResponseEntity.ok(
                recipeService.getRecipeDetail(recipeId)
        );
    }

    @PostMapping
    public ResponseEntity<RecipeCreateResponse> createRecipe(
            Authentication authentication,
            @RequestBody RecipeCreateRequest request
    ) {
        RecipeCreateResponse response = recipeCommandService.createRecipe(
                authentication.getName(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{recipeId:\\d+}")
    public ResponseEntity<RecipeCreateResponse> updateRecipe(
            @PathVariable Long recipeId,
            Authentication authentication,
            @RequestBody RecipeCreateRequest request
    ) {
        return ResponseEntity.ok(
                recipeCommandService.updateRecipe(
                        recipeId,
                        authentication.getName(),
                        request
                )
        );
    }

    @DeleteMapping("/{recipeId:\\d+}")
    public ResponseEntity<Void> deactivateRecipe(
            @PathVariable Long recipeId,
            Authentication authentication
    ) {
        recipeCommandService.deactivateRecipe(
                recipeId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
