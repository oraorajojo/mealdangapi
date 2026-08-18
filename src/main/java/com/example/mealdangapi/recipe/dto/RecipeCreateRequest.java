package com.example.mealdangapi.recipe.dto;

import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class RecipeCreateRequest {

    private ChefCode chefCode;
    private String name;
    private String summary;
    private Integer cookingTimeMin;
    private Integer baseServings;
    private BigDecimal annoyanceScore;
    private Integer knifeLevel;
    private Integer dishCount;
    private String imageUrl;
    private List<MealTime> mealTimes;
    private List<RecipeIngredientCreateRequest> ingredients;
    private List<RecipeStepCreateRequest> steps;
}
