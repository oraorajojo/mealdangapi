package com.example.mealdangapi.recipe.service;

import com.example.mealdangapi.board.repository.BoardPostRepository;
import com.example.mealdangapi.recipe.dto.RecipeDetailResponse;
import com.example.mealdangapi.recipe.dto.RecipeIngredientResponse;
import com.example.mealdangapi.recipe.dto.RecipeListItemResponse;
import com.example.mealdangapi.recipe.dto.RecipeListResponse;
import com.example.mealdangapi.recipe.dto.RecipeStepResponse;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.entity.RecipeMeal;
import com.example.mealdangapi.recipe.repository.RecipeIngredientRepository;
import com.example.mealdangapi.recipe.repository.RecipeMealRepository;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import com.example.mealdangapi.recipe.repository.RecipeSpecification;
import com.example.mealdangapi.recipe.repository.RecipeStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true )
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeMealRepository recipeMealRepository;
    private final BoardPostRepository boardPostRepository;

    public RecipeDetailResponse getRecipeDetail(Long recipeId) {
        Recipe recipe = recipeRepository
                .findByRecipeIdAndActiveTrue(recipeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "활성 레시피를 찾을 수 없습니다."
                ));

        Long postId = boardPostRepository
                .findByRecipeId(recipeId)
                .map(boardPost -> boardPost.getPostId())
                .orElse(null);

        List<MealTime> mealTimes = recipeMealRepository
                .findMealTimesByRecipeId(recipeId);

        List<RecipeIngredientResponse> ingredients = recipeIngredientRepository
                .findAllWithIngredientByRecipeId(recipeId)
                .stream()
                .map(RecipeIngredientResponse::from)
                .toList();

        List<RecipeStepResponse> steps = recipeStepRepository
                .findAllByRecipeIdOrderByStepNo(recipeId)
                .stream()
                .map(RecipeStepResponse::from)
                .toList();

        return RecipeDetailResponse.of(
                recipe,
                postId,
                mealTimes,
                ingredients,
                steps
        );
    }

    public RecipeListResponse getRecipes(
            ChefCode chefCode,
            MealTime mealTime,
            String keyword,
            BigDecimal minAnnoyance,
            BigDecimal maxAnnoyance,
            int page,
            int size
    ) {
        validateListRequest(
                minAnnoyance,
                maxAnnoyance,
                page,
                size
        );

        Specification<Recipe> specification = RecipeSpecification.activeOnly();

        if (chefCode != null) {
            specification = specification.and(
                    RecipeSpecification.chefCodeEquals(chefCode)
            );
        }

        if (mealTime != null) {
            specification = specification.and(
                    RecipeSpecification.mealTimeEquals(mealTime)
            );
        }

        if (StringUtils.hasText(keyword)) {
            specification = specification.and(
                    RecipeSpecification.keywordContains(keyword)
            );
        }

        if (minAnnoyance != null) {
            specification = specification.and(
                    RecipeSpecification.annoyanceScoreAtLeast(minAnnoyance)
            );
        }

        if (maxAnnoyance != null) {
            specification = specification.and(
                    RecipeSpecification.annoyanceScoreAtMost(maxAnnoyance)
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "recipeId")
        );

        Page<Recipe> recipePage = recipeRepository.findAll(
                specification,
                pageRequest
        );

        List<Long> recipeIds = recipePage
                .getContent()
                .stream()
                .map(Recipe::getRecipeId)
                .toList();

        Map<Long, List<String>> mealTimesByRecipeId = getMealTimesByRecipeId(
                recipeIds
        );

        List<RecipeListItemResponse> items = recipePage
                .getContent()
                .stream()
                .map(recipe -> RecipeListItemResponse.of(
                        recipe,
                        mealTimesByRecipeId.getOrDefault(
                                recipe.getRecipeId(),
                                Collections.emptyList()
                        )
                ))
                .toList();

        return new RecipeListResponse(
                items,
                recipePage.getNumber(),
                recipePage.getSize(),
                recipePage.getTotalElements(),
                recipePage.getTotalPages(),
                recipePage.isFirst(),
                recipePage.isLast()
        );
    }

    private Map<Long, List<String>> getMealTimesByRecipeId(
            List<Long> recipeIds
    ) {
        if (recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return recipeMealRepository
                .findAllByRecipeIds(recipeIds)
                .stream()
                .collect(Collectors.groupingBy(
                        recipeMeal -> recipeMeal
                                .getRecipe()
                                .getRecipeId(),
                        Collectors.mapping(
                                recipeMeal -> recipeMeal
                                        .getId()
                                        .getMealTime()
                                        .name(),
                                Collectors.toList()
                        )
                ));
    }

    private void validateListRequest(
            BigDecimal minAnnoyance,
            BigDecimal maxAnnoyance,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page는 0 이상이어야 합니다."
            );
        }

        if (size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size는 1 이상 50 이하여야 합니다."
            );
        }

        if (minAnnoyance != null
                && (minAnnoyance.compareTo(BigDecimal.ONE) < 0
                || minAnnoyance.compareTo(new BigDecimal("5.00")) > 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minAnnoyance는 1.00 이상 5.00 이하여야 합니다."
            );
        }

        if (maxAnnoyance != null
                && (maxAnnoyance.compareTo(BigDecimal.ONE) < 0
                || maxAnnoyance.compareTo(new BigDecimal("5.00")) > 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "maxAnnoyance는 1.00 이상 5.00 이하여야 합니다."
            );
        }

        if (minAnnoyance != null
                && maxAnnoyance != null
                && minAnnoyance.compareTo(maxAnnoyance) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minAnnoyance는 maxAnnoyance보다 클 수 없습니다."
            );
        }
    }
}
