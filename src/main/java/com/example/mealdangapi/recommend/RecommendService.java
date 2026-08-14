package com.example.mealdangapi.recommend;

import com.example.mealdangapi.recommend.dto.FastApiCandidateRecipe;
import com.example.mealdangapi.recommend.dto.FastApiRecipeResult;
import com.example.mealdangapi.recommend.dto.FastApiRecommendRequest;
import com.example.mealdangapi.recommend.dto.FastApiRecommendResponse;
import com.example.mealdangapi.recommend.dto.RecipeResultDto;
import com.example.mealdangapi.recommend.dto.RecommendRequest;
import com.example.mealdangapi.recommend.dto.RecommendResponse;
import com.example.mealdangapi.recommend.dto.SelectRequest;
import com.example.mealdangapi.recommend.dto.SelectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final RecipeRepository recipeRepository;
    private final RecommendLogRepository recommendLogRepository;
    private final RecommendLogResultRepository recommendLogResultRepository;
    private final ChefSelectionRepository chefSelectionRepository;
    private final RestClient fastApiClient;

    public RecommendResponse recommend(RecommendRequest request) {
        List<FastApiCandidateRecipe> candidates = buildCandidates(request.mealTime());

        FastApiRecommendRequest fastApiRequest = new FastApiRecommendRequest(
            request.ingredientsText(),
            request.mealTime(),
            request.conditions() == null ? List.of() : request.conditions(),
            request.excludeIngredients() == null ? List.of() : request.excludeIngredients(),
            request.excludeRecipeIds() == null ? Map.of() : request.excludeRecipeIds(),
            candidates
        );

        FastApiRecommendResponse fastApiResponse = fastApiClient.post()
            .uri("/recommend")
            .body(fastApiRequest)
            .retrieve()
            .body(FastApiRecommendResponse.class);

        RecommendLog savedLog = saveRecommendLog(request);
        Map<String, RecipeResultDto> resultDtos = saveResultsAndBuildResponse(savedLog.getRecommendLogId(), fastApiResponse);

        return new RecommendResponse(
            savedLog.getRecommendLogId(),
            fastApiResponse.parsedIngredients(),
            fastApiResponse.portionHint(),
            resultDtos
        );
    }

    public SelectResponse select(Long recommendLogId, SelectRequest request) {
        if (chefSelectionRepository.existsByRecommendLogId(recommendLogId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 선택된 추천 로그입니다.");
        }
        ChefSelection selection = new ChefSelection();
        selection.setRecommendLogId(recommendLogId);
        selection.setRecipeId(request.recipeId());
        selection.setUserId(request.userId());
        ChefSelection saved = chefSelectionRepository.save(selection);
        return new SelectResponse(saved.getSelectionId());
    }

    // ---------- 내부 로직 ----------

    private List<FastApiCandidateRecipe> buildCandidates(String mealTime) {
        List<RecipeCandidateRow> rows = recipeRepository.findActiveCandidates(mealTime);
        List<Long> recipeIds = rows.stream().map(RecipeCandidateRow::getRecipeId).toList();

        Map<Long, List<String>> ingredientsByRecipe = new HashMap<>();
        if (!recipeIds.isEmpty()) {
            for (RecipeIngredientRow row : recipeRepository.findIngredientNames(recipeIds)) {
                ingredientsByRecipe
                    .computeIfAbsent(row.getRecipeId(), k -> new ArrayList<>())
                    .add(row.getIngredientName());
            }
        }

        return rows.stream()
            .map(r -> new FastApiCandidateRecipe(
                r.getRecipeId(), r.getChefCode(), r.getName(), r.getCookingTimeMin(),
                r.getAnnoyanceScore(), r.getKnifeLevel(), r.getDishCount(), r.getImageUrl(),
                ingredientsByRecipe.getOrDefault(r.getRecipeId(), List.of())
            ))
            .toList();
    }

    private RecommendLog saveRecommendLog(RecommendRequest request) {
        RecommendLog log = new RecommendLog();
        log.setInputIngredientsText(request.ingredientsText());
        log.setMealTime(MealTime.valueOf(request.mealTime()));
        log.setRequestConditions(JsonArrayUtil.toJsonArray(request.conditions()));
        return recommendLogRepository.save(log);
    }

    private Map<String, RecipeResultDto> saveResultsAndBuildResponse(Long recommendLogId, FastApiRecommendResponse fastApiResponse) {
        Map<String, RecipeResultDto> resultDtos = new LinkedHashMap<>();
        int rank = 1;
        for (Map.Entry<String, FastApiRecipeResult> entry : fastApiResponse.results().entrySet()) {
            FastApiRecipeResult r = entry.getValue();
            if (r == null) {
                resultDtos.put(entry.getKey(), null);
                continue;
            }

            RecommendLogResult resultEntity = new RecommendLogResult();
            resultEntity.setRecommendLogId(recommendLogId);
            resultEntity.setChefCode(ChefCode.valueOf(entry.getKey()));
            resultEntity.setRecipeId(r.recipeId());
            resultEntity.setRecommendationRank(rank++);
            resultEntity.setMatchScore(r.matchScore());
            recommendLogResultRepository.save(resultEntity);

            resultDtos.put(entry.getKey(), new RecipeResultDto(
                r.recipeId(), r.name(), r.cookingTimeMin(), r.annoyanceScore(), r.knifeLevel(),
                r.dishCount(), r.imageUrl(), r.ingredients(), r.message(), r.substituteTip(), r.matchScore()
            ));
        }
        return resultDtos;
    }
}
