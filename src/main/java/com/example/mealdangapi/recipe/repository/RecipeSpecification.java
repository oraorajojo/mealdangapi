package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.entity.RecipeMeal;
import com.example.mealdangapi.recipe.entity.RecipeSourceType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

public final class RecipeSpecification {

    private RecipeSpecification() {
    }

    public static Specification<Recipe> activeOnly() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("active"));
    }

    public static Specification<Recipe> chefCodeEquals(
            ChefCode chefCode
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("chefCode"),
                        chefCode
                );
    }

    public static Specification<Recipe> mealTimeEquals(
            MealTime mealTime
    ) {
        return (root, query, criteriaBuilder) -> {
            var subquery = query.subquery(Long.class);
            var recipeMeal = subquery.from(RecipeMeal.class);

            subquery
                    .select(recipeMeal.get("recipe").get("recipeId"))
                    .where(
                            criteriaBuilder.equal(
                                    recipeMeal.get("recipe").get("recipeId"),
                                    root.get("recipeId")
                            ),
                            criteriaBuilder.equal(
                                    recipeMeal.get("id").get("mealTime"),
                                    mealTime
                            )
                    );

            return criteriaBuilder.exists(subquery);
        };
    }

    public static Specification<Recipe> keywordContains(
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            String pattern = "%" + keyword
                    .trim()
                    .toLowerCase(Locale.ROOT) + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    criteriaBuilder.coalesce(
                                            root.get("summary"),
                                            ""
                                    )
                            ),
                            pattern
                    )
            );
        };
    }

    public static Specification<Recipe> annoyanceScoreAtLeast(
            BigDecimal minAnnoyance
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("annoyanceScore"),
                        minAnnoyance
                );
    }

    public static Specification<Recipe> annoyanceScoreAtMost(
            BigDecimal maxAnnoyance
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("annoyanceScore"),
                        maxAnnoyance
                );
    }

    /**
     * 회원이 직접 등록한 레시피(USER_SUBMISSION)는 제외한다.
     * 추천(3셰프 카드)은 관리자 검수를 거친 레시피(ADMIN, FOOD_SAFETY_API)만 노출하고,
     * 회원 레시피는 미식 연구소 게시판에서 별도로 보여준다.
     */
    public static Specification<Recipe> excludeUserSubmission() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(
                        root.get("sourceType"),
                        RecipeSourceType.USER_SUBMISSION
                );
    }
}
