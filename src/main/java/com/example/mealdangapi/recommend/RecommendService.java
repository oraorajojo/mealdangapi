package com.example.mealdangapi.recommend;

import com.example.mealdangapi.recipe.entity.AnnoyanceBand;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.Ingredient;
import com.example.mealdangapi.recipe.entity.IngredientAlias;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.repository.IngredientAliasRepository;
import com.example.mealdangapi.recipe.repository.IngredientRepository;
import com.example.mealdangapi.recipe.repository.RecipeIngredientRepository;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import com.example.mealdangapi.recipe.repository.RecipeSpecification;
import com.example.mealdangapi.recommend.dto.FastApiCandidateRecipe;
import com.example.mealdangapi.recommend.dto.FastApiIngredientDictionaryEntry;
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
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository ingredientAliasRepository;
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
            candidates,
            buildIngredientDictionary()
        );

        FastApiRecommendResponse fastApiResponse = fastApiClient.post()
            .uri("/recommend")
            .body(fastApiRequest)
            .retrieve()
            .body(FastApiRecommendResponse.class);

        RecommendLog savedLog = saveRecommendLog(request, mealTime, band, resolveUserId(authentication), fastApiResponse.normalizedIngredientIds());
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

    // 예전엔 KOREAN/CHINESE/WESTERN마다 따로 조회해서 쿼리가 6번(레시피+재료 각 3번) 나갔다.
    // chefCode는 FastAPI로 보내는 각 후보 데이터에 이미 담겨 있어서(FastApiCandidateRecipe.chefCode),
    // 셰프별로 나눠 조회할 필요 없이 조건에 맞는 레시피를 한 번에 다 가져와도 결과는 똑같다.
    // (셰프별로 후보를 나누는 건 FastAPI의 pick_best()가 알아서 한다)
    private List<FastApiCandidateRecipe> buildCandidates(MealTime mealTime, AnnoyanceBand band) {
        Specification<Recipe> spec = Specification
            .where(RecipeSpecification.activeOnly())
            .and(RecipeSpecification.excludeUserSubmission());

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
            return List.of();
        }

        List<Long> recipeIds = recipes.stream().map(Recipe::getRecipeId).toList();
        Map<Long, List<String>> ingredientsByRecipeId = recipeIngredientRepository
            .findAllWithIngredientByRecipeIds(recipeIds)
            .stream()
            .collect(Collectors.groupingBy(
                ri -> ri.getRecipe().getRecipeId(),
                Collectors.mapping(ri -> ri.getIngredient().getName(), Collectors.toList())
            ));

        return recipes.stream()
            .map(r -> new FastApiCandidateRecipe(
                r.getRecipeId(), r.getChefCode().name(), r.getName(), r.getCookingTimeMin(),
                r.getAnnoyanceScore(), r.getKnifeLevel(), r.getDishCount(), r.getImageUrl(),
                ingredientsByRecipeId.getOrDefault(r.getRecipeId(), List.of())
            ))
            .toList();
    }

    // 재료(1099개)+별칭(146개) 전체가 요청마다 거의 안 바뀌는 데이터인데도 매번 DB에서
    // 새로 긁어오고 있었다(쿼리 2번). 짧은 TTL로 메모리에 캐싱해서 그 2번을 없앤다.
    // 새 재료/별칭이 추가돼도 최대 TTL만큼만 늦게 반영되는 정도라 추천 정확도엔 문제없다.
    private static final long DICTIONARY_CACHE_TTL_MILLIS = 5 * 60 * 1000L;
    private volatile List<FastApiIngredientDictionaryEntry> cachedDictionary;
    private volatile long dictionaryCachedAtMillis;

    // FastAPI는 DB에 접근하지 않으므로, 재료 인식에 쓸 사전(표준명 + 별칭)을 매 요청마다 통째로 넘긴다.
    // 표준명은 term==canonicalName인 자기 자신 항목으로, 별칭은 term=별칭 / canonicalName=연결된 표준명으로 넣는다.
    private List<FastApiIngredientDictionaryEntry> buildIngredientDictionary() {
        List<FastApiIngredientDictionaryEntry> cached = cachedDictionary;
        long now = System.currentTimeMillis();
        if (cached != null && now - dictionaryCachedAtMillis < DICTIONARY_CACHE_TTL_MILLIS) {
            return cached;
        }

        List<FastApiIngredientDictionaryEntry> dictionary = new java.util.ArrayList<>();

        for (Ingredient ingredient : ingredientRepository.findAll()) {
            dictionary.add(new FastApiIngredientDictionaryEntry(
                ingredient.getName(), ingredient.getIngredientId(), ingredient.getName()
            ));
        }

        for (IngredientAlias alias : ingredientAliasRepository.findAllWithIngredient()) {
            Ingredient ingredient = alias.getIngredient();
            dictionary.add(new FastApiIngredientDictionaryEntry(
                alias.getAlias(), ingredient.getIngredientId(), ingredient.getName()
            ));
        }

        cachedDictionary = dictionary;
        dictionaryCachedAtMillis = now;

        return dictionary;
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

    private RecommendLog saveRecommendLog(RecommendRequest request, MealTime mealTime, AnnoyanceBand band, Long userId, List<Long> normalizedIngredientIds) {
        RecommendLog log = new RecommendLog();
        log.setUserId(userId);
        log.setInputIngredientsText(request.ingredientsText());
        log.setMealTime(mealTime);
        log.setAnnoyanceBand(band);
        log.setRequestConditions(JsonArrayUtil.toJsonArray(request.conditions()));
        log.setNormalizedIngredientIds(JsonArrayUtil.toJsonArrayOfNumbers(normalizedIngredientIds));
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
