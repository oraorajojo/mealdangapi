package com.example.mealdangapi.chef.service;

import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChefProfileService {

    private static final List<String> CHEF_CODES = List.of(
            "KOREAN",
            "CHINESE",
            "WESTERN"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public ChefStatisticsResponse getChefStatistics(
            String userEmail,
            String periodValue
    ) {
        Long userId = requireUserId(userEmail);
        PeriodRange period = PeriodRange.from(periodValue);
        MapSqlParameterSource parameters = createSelectionParameters(userId, period);

        Map<String, Object> summaryRow = jdbcTemplate.queryForMap(
                """
                SELECT
                    COUNT(cs.selection_id) AS selectionCount,
                    AVG(r.cooking_time_min) AS averageCookingTimeMin,
                    AVG(r.annoyance_score) AS averageAnnoyanceScore,
                    AVG(r.dish_count) AS averageDishCount
                FROM chef_selections cs
                JOIN recipes r
                  ON r.recipe_id = cs.recipe_id
                WHERE %s
                """.formatted(selectionWhereClause(period)),
                parameters
        );

        long selectionCount = longValue(summaryRow.get("selectionCount"));
        Integer averageCookingTimeMin = roundedInteger(
                decimalValue(summaryRow.get("averageCookingTimeMin"))
        );
        BigDecimal averageAnnoyanceScore = roundedDecimal(
                decimalValue(summaryRow.get("averageAnnoyanceScore"))
        );
        BigDecimal averageDishCount = roundedDecimal(
                decimalValue(summaryRow.get("averageDishCount"))
        );

        List<ChefDistributionResponse> chefDistribution =
                getChefDistribution(period, parameters, selectionCount);

        ChefDistributionResponse topChef = chefDistribution.stream()
                .max(Comparator.comparingLong(
                        ChefDistributionResponse::selectionCount
                ))
                .filter(item -> item.selectionCount() > 0)
                .orElse(null);

        String favoriteMealTime = getFavoriteMealTime(period, parameters);
        List<RecentSelectionResponse> recentSelections =
                getRecentSelections(period, parameters);

        return new ChefStatisticsResponse(
                period.code(),
                period.label(),
                selectionCount,
                topChef == null ? null : topChef.chefCode(),
                topChef == null ? "아직 담당 셰프를 정하는 중이에요."
                        : topChef.chefName(),
                buildNote(period, selectionCount, topChef),
                chefDistribution,
                averageCookingTimeMin,
                averageAnnoyanceScore,
                averageDishCount,
                buildTasteTitle(averageAnnoyanceScore),
                buildTasteDescription(
                        selectionCount,
                        averageAnnoyanceScore,
                        averageDishCount
                ),
                favoriteMealTime,
                recentSelections
        );
    }

    public List<MyRecipeResponse> getMyRecipes(
            String userEmail,
            int size
    ) {
        Long userId = requireUserId(userEmail);

        if (size < 1 || size > 50) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "size는 1~50 사이여야 합니다."
            );
        }

        String sql = """
                SELECT
                    r.recipe_id AS recipeId,
                    r.chef_code AS chefCode,
                    r.name AS name,
                    r.summary AS summary,
                    r.cooking_time_min AS cookingTimeMin,
                    r.image_url AS imageUrl,
                    r.source_type AS sourceType,
                    r.is_active AS active,
                    r.created_at AS createdAt
                FROM recipes r
                WHERE r.author_user_id = :userId
                ORDER BY r.created_at DESC, r.recipe_id DESC
                LIMIT :size
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("size", size);

        return jdbcTemplate.queryForList(sql, parameters)
                .stream()
                .map(row -> new MyRecipeResponse(
                        longValue(row.get("recipeId")),
                        stringValue(row.get("chefCode")),
                        chefName(stringValue(row.get("chefCode"))),
                        stringValue(row.get("name")),
                        stringValue(row.get("summary")),
                        integerValue(row.get("cookingTimeMin")),
                        stringValue(row.get("imageUrl")),
                        stringValue(row.get("sourceType")),
                        booleanValue(row.get("active")),
                        localDateTimeValue(row.get("createdAt"))
                ))
                .toList();
    }

    private List<ChefDistributionResponse> getChefDistribution(
            PeriodRange period,
            MapSqlParameterSource parameters,
            long totalSelectionCount
    ) {
        String sql = """
                SELECT
                    r.chef_code AS chefCode,
                    COUNT(cs.selection_id) AS selectionCount
                FROM chef_selections cs
                JOIN recipes r
                  ON r.recipe_id = cs.recipe_id
                WHERE %s
                  AND r.chef_code IN ('KOREAN', 'CHINESE', 'WESTERN')
                GROUP BY r.chef_code
                """.formatted(selectionWhereClause(period));

        Map<String, Long> countByChefCode = new LinkedHashMap<>();
        for (String chefCode : CHEF_CODES) {
            countByChefCode.put(chefCode, 0L);
        }

        for (Map<String, Object> row : jdbcTemplate.queryForList(sql, parameters)) {
            String chefCode = stringValue(row.get("chefCode"));
            if (countByChefCode.containsKey(chefCode)) {
                countByChefCode.put(
                        chefCode,
                        longValue(row.get("selectionCount"))
                );
            }
        }

        return countByChefCode.entrySet().stream()
                .map(entry -> new ChefDistributionResponse(
                        entry.getKey(),
                        chefName(entry.getKey()),
                        entry.getValue(),
                        percentage(entry.getValue(), totalSelectionCount)
                ))
                .toList();
    }

    private String getFavoriteMealTime(
            PeriodRange period,
            MapSqlParameterSource parameters
    ) {
        String sql = """
                SELECT
                    rl.meal_time AS mealTime,
                    COUNT(cs.selection_id) AS selectionCount
                FROM chef_selections cs
                JOIN recommend_logs rl
                  ON rl.recommend_log_id = cs.recommend_log_id
                WHERE %s
                  AND rl.meal_time IS NOT NULL
                GROUP BY rl.meal_time
                ORDER BY
                    selectionCount DESC,
                    FIELD(
                        rl.meal_time,
                        'BREAKFAST',
                        'LUNCH',
                        'DINNER',
                        'LATE_NIGHT'
                    ) ASC
                LIMIT 1
                """.formatted(selectionWhereClause(period));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);
        if (rows.isEmpty()) {
            return null;
        }

        return mealTimeName(stringValue(rows.get(0).get("mealTime")));
    }

    private List<RecentSelectionResponse> getRecentSelections(
            PeriodRange period,
            MapSqlParameterSource parameters
    ) {
        String sql = """
                SELECT
                    cs.selection_id AS selectionId,
                    cs.recommend_log_id AS recommendLogId,
                    cs.selected_at AS selectedAt,
                    r.recipe_id AS recipeId,
                    r.name AS recipeName,
                    r.chef_code AS chefCode,
                    r.cooking_time_min AS cookingTimeMin,
                    r.annoyance_score AS annoyanceScore,
                    r.dish_count AS dishCount,
                    rl.meal_time AS mealTime
                FROM chef_selections cs
                JOIN recipes r
                  ON r.recipe_id = cs.recipe_id
                LEFT JOIN recommend_logs rl
                  ON rl.recommend_log_id = cs.recommend_log_id
                WHERE %s
                ORDER BY cs.selected_at DESC, cs.selection_id DESC
                LIMIT 6
                """.formatted(selectionWhereClause(period));

        return jdbcTemplate.queryForList(sql, parameters)
                .stream()
                .map(row -> new RecentSelectionResponse(
                        longValue(row.get("selectionId")),
                        longValue(row.get("recommendLogId")),
                        longValue(row.get("recipeId")),
                        stringValue(row.get("recipeName")),
                        stringValue(row.get("chefCode")),
                        chefName(stringValue(row.get("chefCode"))),
                        mealTimeName(stringValue(row.get("mealTime"))),
                        integerValue(row.get("cookingTimeMin")),
                        roundedDecimal(decimalValue(row.get("annoyanceScore"))),
                        integerValue(row.get("dishCount")),
                        localDateTimeValue(row.get("selectedAt"))
                ))
                .toList();
    }

    private Long requireUserId(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByEmail(userEmail)
                .map(User::getUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private MapSqlParameterSource createSelectionParameters(
            Long userId,
            PeriodRange period
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId);

        if (period.startedAt() != null) {
            parameters.addValue("startedAt", period.startedAt());
        }

        return parameters;
    }

    private String selectionWhereClause(PeriodRange period) {
        if (period.startedAt() == null) {
            return "cs.user_id = :userId";
        }

        return "cs.user_id = :userId AND cs.selected_at >= :startedAt";
    }

    private String buildNote(
            PeriodRange period,
            long selectionCount,
            ChefDistributionResponse topChef
    ) {
        if (selectionCount == 0 || topChef == null) {
            return "아직 메뉴 선택 기록이 없어요. 추천 메뉴를 선택하면 취향이 쌓입니다.";
        }

        return "%s 동안 %s을(를) %d번 가장 많이 선택했어요."
                .formatted(
                        period.label(),
                        topChef.chefName(),
                        topChef.selectionCount()
                );
    }

    private String buildTasteTitle(BigDecimal averageAnnoyanceScore) {
        if (averageAnnoyanceScore == null) {
            return "아직 조리 부담 성향을 분석 중이에요";
        }

        if (averageAnnoyanceScore.compareTo(new BigDecimal("2.00")) <= 0) {
            return "간단한 메뉴 선호";
        }

        if (averageAnnoyanceScore.compareTo(new BigDecimal("3.50")) <= 0) {
            return "적당한 조리 부담 선호";
        }

        return "완성도 있는 메뉴 선호";
    }

    private String buildTasteDescription(
            long selectionCount,
            BigDecimal averageAnnoyanceScore,
            BigDecimal averageDishCount
    ) {
        if (selectionCount == 0 || averageAnnoyanceScore == null) {
            return "선택 기록이 쌓이면 귀찮음과 설거지 지수를 바탕으로 취향을 분석합니다.";
        }

        String dishText = averageDishCount == null
                ? "설거지 정보 없음"
                : "평균 설거지 %s개".formatted(
                averageDishCount.stripTrailingZeros().toPlainString()
        );

        return "선택한 메뉴의 평균 귀찮음 %s점 · %s"
                .formatted(
                        averageAnnoyanceScore.stripTrailingZeros().toPlainString(),
                        dishText
                );
    }

    private int percentage(long count, long total) {
        if (total == 0) {
            return 0;
        }

        return (int) Math.round((count * 100.0) / total);
    }

    private String chefName(String chefCode) {
        if (chefCode == null) {
            return null;
        }

        return switch (chefCode) {
            case "KOREAN" -> "한식 셰프";
            case "CHINESE" -> "중식 셰프";
            case "WESTERN" -> "양식 셰프";
            default -> "기타 셰프";
        };
    }

    private String mealTimeName(String mealTime) {
        if (mealTime == null) {
            return null;
        }

        return switch (mealTime) {
            case "BREAKFAST" -> "아침";
            case "LUNCH" -> "점심";
            case "DINNER" -> "저녁";
            case "LATE_NIGHT" -> "야식";
            default -> null;
        };
    }

    private long longValue(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private Integer integerValue(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        return BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private BigDecimal roundedDecimal(BigDecimal value) {
        return value == null
                ? null
                : value.setScale(1, RoundingMode.HALF_UP);
    }

    private Integer roundedInteger(BigDecimal value) {
        return value == null
                ? null
                : value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        return value != null && ((Number) value).intValue() != 0;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime localDateTimeValue(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        return null;
    }

    private record PeriodRange(
            String code,
            String label,
            LocalDateTime startedAt
    ) {
        private static PeriodRange from(String rawValue) {
            String value = rawValue == null
                    ? "month"
                    : rawValue.trim().toLowerCase(Locale.ROOT);

            LocalDateTime now = LocalDateTime.now();

            return switch (value) {
                case "week" -> new PeriodRange(
                        "week",
                        "최근 7일",
                        now.minusDays(7)
                );
                case "month" -> new PeriodRange(
                        "month",
                        "최근 30일",
                        now.minusDays(30)
                );
                case "all" -> new PeriodRange(
                        "all",
                        "전체 기간",
                        null
                );
                default -> throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "period는 week, month, all 중 하나여야 합니다."
                );
            };
        }
    }

    public record ChefStatisticsResponse(
            String period,
            String periodLabel,
            long selectionCount,
            String topChefCode,
            String topChefName,
            String note,
            List<ChefDistributionResponse> chefDistribution,
            Integer averageCookingTimeMin,
            BigDecimal averageAnnoyanceScore,
            BigDecimal averageDishCount,
            String tasteTitle,
            String tasteDescription,
            String favoriteMealTime,
            List<RecentSelectionResponse> recentSelections
    ) {
    }

    public record ChefDistributionResponse(
            String chefCode,
            String chefName,
            long selectionCount,
            int percentage
    ) {
    }

    public record RecentSelectionResponse(
            Long selectionId,
            Long recommendLogId,
            Long recipeId,
            String recipeName,
            String chefCode,
            String chefName,
            String mealTime,
            Integer cookingTimeMin,
            BigDecimal annoyanceScore,
            Integer dishCount,
            LocalDateTime selectedAt
    ) {
    }

    public record MyRecipeResponse(
            Long recipeId,
            String chefCode,
            String chefName,
            String name,
            String summary,
            Integer cookingTimeMin,
            String imageUrl,
            String sourceType,
            boolean active,
            LocalDateTime createdAt
    ) {
    }
}
