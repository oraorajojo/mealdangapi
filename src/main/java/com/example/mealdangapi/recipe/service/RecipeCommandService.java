package com.example.mealdangapi.recipe.service;

import com.example.mealdangapi.board.api.BoardPostCommandApi;
import com.example.mealdangapi.recipe.dto.RecipeCreateRequest;
import com.example.mealdangapi.recipe.dto.RecipeCreateResponse;
import com.example.mealdangapi.recipe.dto.RecipeIngredientCreateRequest;
import com.example.mealdangapi.recipe.dto.RecipeStepCreateRequest;
import com.example.mealdangapi.recipe.entity.Ingredient;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.entity.RecipeIngredient;
import com.example.mealdangapi.recipe.entity.RecipeMeal;
import com.example.mealdangapi.recipe.entity.RecipeSourceType;
import com.example.mealdangapi.recipe.entity.RecipeStep;
import com.example.mealdangapi.recipe.repository.IngredientRepository;
import com.example.mealdangapi.recipe.repository.RecipeIngredientRepository;
import com.example.mealdangapi.recipe.repository.RecipeMealRepository;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import com.example.mealdangapi.recipe.repository.RecipeStepRepository;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeCommandService {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeMealRepository recipeMealRepository;
    private final BoardPostCommandApi boardPostCommandApi;

    @Transactional
    public RecipeCreateResponse createRecipe(
            String userEmail,
            RecipeCreateRequest request
    ) {
        validateRequest(request);

        User author = findUserByEmail(userEmail);
        Map<Long, Ingredient> ingredientsById = findAndValidateIngredients(
                request.getIngredients()
        );

        Recipe recipe = Recipe.createUserRecipe(
                author,
                request.getChefCode(),
                request.getName().trim(),
                normalizeOptionalText(request.getSummary()),
                request.getCookingTimeMin(),
                request.getBaseServings(),
                request.getAnnoyanceScore(),
                request.getKnifeLevel(),
                request.getDishCount(),
                normalizeOptionalText(request.getImageUrl())
        );

        Recipe savedRecipe = recipeRepository.save(recipe);
        saveChildData(savedRecipe, request, ingredientsById);
        createBoardPostForUserRecipe(savedRecipe, author);

        return new RecipeCreateResponse(
                savedRecipe.getRecipeId(),
                "레시피가 등록되었습니다."
        );
    }

    @Transactional
    public RecipeCreateResponse updateRecipe(
            Long recipeId,
            String userEmail,
            RecipeCreateRequest request
    ) {
        validateRequest(request);

        Recipe recipe = findOwnedActiveUserRecipe(recipeId, userEmail);
        Map<Long, Ingredient> ingredientsById = findAndValidateIngredients(
                request.getIngredients()
        );

        recipe.updateUserRecipe(
                request.getChefCode(),
                request.getName().trim(),
                normalizeOptionalText(request.getSummary()),
                request.getCookingTimeMin(),
                request.getBaseServings(),
                request.getAnnoyanceScore(),
                request.getKnifeLevel(),
                request.getDishCount(),
                normalizeOptionalText(request.getImageUrl())
        );

        recipeIngredientRepository.deleteAllByRecipeId(recipeId);
        recipeStepRepository.deleteAllByRecipeId(recipeId);
        recipeMealRepository.deleteAllByRecipeId(recipeId);

        saveChildData(recipe, request, ingredientsById);

        // board_posts.title/content는 생성 시점에 recipes.name/summary를 복사해 온
        // 값이라, 레시피만 고치면 게시판 목록엔 옛 제목이 남는다. 연결된 게시글이
        // 있으면 같이 갱신한다(없으면 boardPostCommandApi가 조용히 넘어간다).
        boardPostCommandApi.updatePostContent(recipe.getRecipeId());

        return new RecipeCreateResponse(
                recipe.getRecipeId(),
                "레시피가 수정되었습니다."
        );
    }

    @Transactional
    public void deactivateRecipe(
            Long recipeId,
            String userEmail
    ) {
        Recipe recipe = findOwnedActiveUserRecipe(recipeId, userEmail);
        recipe.deactivate();
    }

    private void createBoardPostForUserRecipe(
            Recipe recipe,
            User author
    ) {
        if (recipe.getSourceType() != RecipeSourceType.USER_SUBMISSION) {
            return;
        }

        boardPostCommandApi.createBoardPost(
                recipe.getRecipeId(),
                author.getUserId()
        );
    }

    private User findUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "인증 사용자를 찾을 수 없습니다."
                ));
    }

    private Recipe findOwnedActiveUserRecipe(
            Long recipeId,
            String userEmail
    ) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "레시피를 찾을 수 없습니다."
                ));

        if (!recipe.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "활성 레시피를 찾을 수 없습니다."
            );
        }

        if (recipe.getSourceType() != RecipeSourceType.USER_SUBMISSION
                || recipe.getAuthor() == null
                || !recipe.getAuthor().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인이 등록한 레시피만 수정하거나 비활성화할 수 있습니다."
            );
        }

        return recipe;
    }

    private Map<Long, Ingredient> findAndValidateIngredients(
            List<RecipeIngredientCreateRequest> ingredientRequests
    ) {
        List<Long> ingredientIds = ingredientRequests
                .stream()
                .map(RecipeIngredientCreateRequest::getIngredientId)
                .toList();

        List<Ingredient> ingredients = ingredientRepository.findAllById(
                ingredientIds
        );

        if (ingredients.size() != ingredientIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않는 재료가 포함되어 있습니다."
            );
        }

        return ingredients
                .stream()
                .collect(Collectors.toMap(
                        Ingredient::getIngredientId,
                        Function.identity()
                ));
    }

    private void saveChildData(
            Recipe recipe,
            RecipeCreateRequest request,
            Map<Long, Ingredient> ingredientsById
    ) {
        saveRecipeIngredients(
                recipe,
                request.getIngredients(),
                ingredientsById
        );
        saveRecipeSteps(recipe, request.getSteps());
        saveRecipeMeals(recipe, request.getMealTimes());
    }

    private void saveRecipeIngredients(
            Recipe recipe,
            List<RecipeIngredientCreateRequest> requests,
            Map<Long, Ingredient> ingredientsById
    ) {
        List<RecipeIngredient> recipeIngredients = new ArrayList<>();

        for (int index = 0; index < requests.size(); index++) {
            RecipeIngredientCreateRequest request = requests.get(index);

            recipeIngredients.add(RecipeIngredient.create(
                    recipe,
                    ingredientsById.get(request.getIngredientId()),
                    request.getAmountValue(),
                    normalizeOptionalText(request.getAmountUnit()),
                    request.isEssential(),
                    index + 1
            ));
        }

        recipeIngredientRepository.saveAll(recipeIngredients);
    }

    private void saveRecipeSteps(
            Recipe recipe,
            List<RecipeStepCreateRequest> requests
    ) {
        List<RecipeStep> recipeSteps = new ArrayList<>();

        for (int index = 0; index < requests.size(); index++) {
            RecipeStepCreateRequest request = requests.get(index);

            recipeSteps.add(RecipeStep.create(
                    recipe,
                    index + 1,
                    request.getContent().trim(),
                    normalizeOptionalText(request.getImageUrl())
            ));
        }

        recipeStepRepository.saveAll(recipeSteps);
    }

    private void saveRecipeMeals(
            Recipe recipe,
            List<MealTime> mealTimes
    ) {
        List<RecipeMeal> recipeMeals = mealTimes
                .stream()
                .map(mealTime -> RecipeMeal.create(recipe, mealTime))
                .toList();

        recipeMealRepository.saveAll(recipeMeals);
    }

    private void validateRequest(RecipeCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "레시피 등록 요청이 필요합니다."
            );
        }

        if (request.getChefCode() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "셰프 분류를 선택해주세요."
            );
        }

        validateRequiredText(
                request.getName(),
                "레시피 이름은 필수입니다.",
                200
        );
        validateOptionalText(
                request.getSummary(),
                "레시피 요약은 1000자 이하여야 합니다.",
                1000
        );
        validateOptionalText(
                request.getImageUrl(),
                "대표 이미지 URL은 1000자 이하여야 합니다.",
                1000
        );

        if (request.getCookingTimeMin() == null
                || request.getCookingTimeMin() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "조리 시간은 1분 이상이어야 합니다."
            );
        }

        if (request.getBaseServings() == null
                || request.getBaseServings() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "기준 인분은 1 이상이어야 합니다."
            );
        }

        if (request.getAnnoyanceScore() == null
                || request.getAnnoyanceScore().compareTo(BigDecimal.ONE) < 0
                || request.getAnnoyanceScore()
                .compareTo(new BigDecimal("5.00")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "귀찮음 지수는 1.00 이상 5.00 이하여야 합니다."
            );
        }

        if (request.getKnifeLevel() == null
                || request.getKnifeLevel() < 0
                || request.getKnifeLevel() > 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "칼질 지수는 0 이상 3 이하여야 합니다."
            );
        }

        if (request.getDishCount() == null
                || request.getDishCount() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "설거지 개수는 0 이상이어야 합니다."
            );
        }

        validateMeals(request.getMealTimes());
        validateIngredients(request.getIngredients());
        validateSteps(request.getSteps());
    }

    private void validateMeals(List<MealTime> mealTimes) {
        if (mealTimes == null || mealTimes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "최소 한 개의 추천 시간대를 선택해주세요."
            );
        }

        if (new HashSet<>(mealTimes).size() != mealTimes.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "추천 시간대가 중복되었습니다."
            );
        }
    }

    private void validateIngredients(
            List<RecipeIngredientCreateRequest> ingredients
    ) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "최소 한 개의 재료를 입력해주세요."
            );
        }

        Set<Long> ingredientIds = new HashSet<>();

        for (RecipeIngredientCreateRequest ingredient : ingredients) {
            if (ingredient == null || ingredient.getIngredientId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "재료 ID는 필수입니다."
                );
            }

            if (!ingredientIds.add(ingredient.getIngredientId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "같은 재료를 중복 등록할 수 없습니다."
                );
            }

            if (ingredient.getAmountValue() != null
                    && ingredient.getAmountValue()
                    .compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "재료 수량은 0 이상이어야 합니다."
                );
            }

            validateOptionalText(
                    ingredient.getAmountUnit(),
                    "재료 단위는 30자 이하여야 합니다.",
                    30
            );
        }
    }

    private void validateSteps(List<RecipeStepCreateRequest> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "최소 한 개의 조리 단계를 입력해주세요."
            );
        }

        for (RecipeStepCreateRequest step : steps) {
            if (step == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "조리 단계 정보가 올바르지 않습니다."
                );
            }

            validateRequiredText(
                    step.getContent(),
                    "조리 단계 내용은 필수입니다.",
                    65535
            );
            validateOptionalText(
                    step.getImageUrl(),
                    "조리 단계 이미지 URL은 1000자 이하여야 합니다.",
                    1000
            );
        }
    }

    private void validateRequiredText(
            String value,
            String errorMessage,
            int maxLength
    ) {
        if (!StringUtils.hasText(value)
                || value.trim().length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorMessage
            );
        }
    }

    private void validateOptionalText(
            String value,
            String errorMessage,
            int maxLength
    ) {
        if (StringUtils.hasText(value)
                && value.trim().length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorMessage
            );
        }
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }
}
