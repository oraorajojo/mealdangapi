package com.example.mealdangapi.recommend;

import com.example.mealdangapi.recipe.entity.AnnoyanceBand;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.repository.RecipeIngredientRepository;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import com.example.mealdangapi.recipe.repository.RecipeSpecification;
import com.example.mealdangapi.recommend.dto.FastApiCandidateRecipe;
import com.example.mealdangapi.recommend.dto.FastApiRecipeResult;
import com.example.mealdangapi.recommend.dto.FastApiRecommendRequest;
import com.example.mealdangapi.recommend.dto.FastApiRecommendResponse;
import com.example.mealdangapi.recommend.dto.RecipeResultDto;
import com.example.mealdangapi.recommend.dto.RecommendRequest;
import com.example.mealdangapi.recommend.dto.RecommendResponse;
import com.example.mealdangapi.recommend.dto.SelectRequest;
import com.example.mealdangapi.recommend.dto.SelectResponse;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendService {

    // 후보 조회는 recipe 도메인(치연 담당)의 엔티티·Specification을 그대로 재사용한다.
    // 단, RecipeCandidateService(치연 작성)는 chefCode/mealTime/annoyanceBand가 전부 필수라
    // "미세조정 없이도 검색" 요구사항과 안 맞아서, 여기서는 RecipeRepository + Specification을
    // 직접 조합해 mealTime/annoyanceBand를 선택적으로 적용한다.
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecommendLogRepository recommendLogRepository;
    private final RecommendLogResultRepository recommendLogResultRepository;
    private final ChefSelectionRepository chefSelectionRepository;
    private final UserRepository userRepository;
    private final RestClient fastApiClient;

    public RecommendResponse recommend(Authentication authentication, RecommendRequest request) {
        MealTime mealTime = parseMealTime(request.mealTime());
        AnnoyanceBand band = parseAnnoyanceBand(request.annoyanceBand());

        List<FastApiCandidateRecipe> candidates = buildCandidates(mealTime, band);

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

        RecommendLog savedLog = saveRecommendLog(request, mealTime, band, resolveUserId(authentication));
        Map<String, RecipeResultDto> resultDtos = saveResultsAndBuildResponse(savedLog.getRecommendLogId(), fastApiResponse);

        return new RecommendResponse(
            savedLog.getRecommendLogId(),
            fastApiResponse.parsedIngredients(),
            fastApiResponse.portionHint(),
            resultDtos
        );
    }

    public SelectResponse select(Long recommendLogId, String userEmail, SelectRequest request) {
        Long userId = userRepository.findByEmail(userEmail)
            .map(User::getUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        RecommendLog log = recommendLogRepository.findById(recommendLogId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "추천 로그를 찾을 수 없습니다."));

        // 비회원(user_id NULL) 추천 로그는 소유자가 없어 로그인 후에도 선택 대상이 될 수 없다.
        // (비회원은 추천·결과 열람까지만 가능 — 선택하려면 로그인 후 다시 추천받아야 함)
        if (!userId.equals(log.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 추천 로그가 아닙니다.");
        }

        if (!recommendLogResultRepository.existsByRecommendLogIdAndRecipeId(recommendLogId, request.recipeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 추천 로그의 후보 레시피가 아닙니다.");
        }

        if (chefSelectionRepository.existsByRecommendLogId(recommendLogId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 선택된 추천 로그입니다.");
        }

        ChefSelection selection = new ChefSelection();
        selection.setRecommendLogId(recommendLogId);
        selection.setRecipeId(request.recipeId());
        selection.setUserId(userId);
        ChefSelection saved = chefSelectionRepository.save(selection);
        return new SelectResponse(saved.getSelectionId());
    }

    // ---------- 내부 로직 ----------

    private MealTime parseMealTime(String value) {
        return (value == null || value.isBlank()) ? null : MealTime.valueOf(value);
    }

    private AnnoyanceBand parseAnnoyanceBand(String value) {
        return (value == null || value.isBlank()) ? null : AnnoyanceBand.valueOf(value);
    }

    private List<FastApiCandidateRecipe> buildCandidates(MealTime mealTime, AnnoyanceBand band) {
        List<FastApiCandidateRecipe> candidates = new java.util.ArrayList<>();

        for (ChefCode chefCode : List.of(ChefCode.KOREAN, ChefCode.CHINESE, ChefCode.WESTERN)) {
            Specification<Recipe> spec = Specification
                .where(RecipeSpecification.activeOnly())
                .and(RecipeSpecification.chefCodeEquals(chefCode));

            if (mealTime != null) {
                spec = spec.and(RecipeSpecification.mealTimeEquals(mealTime));
            }
            if (band != null) {
                spec = spec
                    .and(RecipeSpecification.annoyanceScoreAtLeast(band.getMinInclusive()))
                    .and(annoyanceScoreLessThan(band.getMaxExclusive()));
            }

            List<Recipe> recipes = recipeRepository.findAll(spec);
            if (recipes.isEmpty()) {
                continue;
            }

            List<Long> recipeIds = recipes.stream().map(Recipe::getRecipeId).toList();
            Map<Long, List<String>> ingredientsByRecipeId = recipeIngredientRepository
                .findAllWithIngredientByRecipeIds(recipeIds)
                .stream()
                .collect(Collectors.groupingBy(
                    ri -> ri.getRecipe().getRecipeId(),
                    Collectors.mapping(ri -> ri.getIngredient().getName(), Collectors.toList())
                ));

            for (Recipe r : recipes) {
                candidates.add(new FastApiCandidateRecipe(
                    r.getRecipeId(), r.getChefCode().name(), r.getName(), r.getCookingTimeMin(),
                    r.getAnnoyanceScore(), r.getKnifeLevel(), r.getDishCount(), r.getImageUrl(),
                    ingredientsByRecipeId.getOrDefault(r.getRecipeId(), List.of())
                ));
            }
        }

        return candidates;
    }

    // RecipeSpecification에는 상한을 "이하(<=)"로 거는 것만 있어서, annoyanceBand의
    // 배타적 상한(maxExclusive, 예: MID는 4.00 미만)을 그대로 반영하기 위해 여기서만 쓰는 조건.
    private Specification<Recipe> annoyanceScoreLessThan(BigDecimal maxExclusive) {
        return (root, query, cb) -> cb.lessThan(root.get("annoyanceScore"), maxExclusive);
    }

    // 인증 필터는 토큰이 있으면 permitAll 엔드포인트에서도 인증 정보를 채워준다.
    // 로그인 상태로 요청했다면 recommend_logs.user_id를 남겨서, 이후 select에서
    // 본인 로그인지 검증할 수 있게 한다 (비로그인이면 null로 남아 select가 불가능해짐 - 의도된 동작).
    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName())
            .map(User::getUserId)
            .orElse(null);
    }

    private RecommendLog saveRecommendLog(RecommendRequest request, MealTime mealTime, AnnoyanceBand band, Long userId) {
        RecommendLog log = new RecommendLog();
        log.setUserId(userId);
        log.setInputIngredientsText(request.ingredientsText());
        log.setMealTime(mealTime);
        log.setAnnoyanceBand(band);
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
