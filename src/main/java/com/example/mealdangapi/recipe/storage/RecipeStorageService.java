package com.example.mealdangapi.recipe.storage;

import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true )
public class RecipeStorageService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    @Transactional
    public void addBookmark(String userEmail, Long recipeId) {
        Long userId = requireUserId(userEmail);
        validateActiveRecipe(recipeId);

        String sql = """
                INSERT IGNORE INTO recipe_bookmarks (
                    user_id,
                    recipe_id,
                    saved_at
                ) VALUES (
                    :userId,
                    :recipeId,
                    CURRENT_TIMESTAMP
                )
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("recipeId", recipeId)
        );
    }

    @Transactional
    public void removeBookmark(String userEmail, Long recipeId) {
        Long userId = requireUserId(userEmail);

        String sql = """
                DELETE FROM recipe_bookmarks
                WHERE user_id = :userId
                  AND recipe_id = :recipeId
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("recipeId", recipeId)
        );
    }

    /** 레시피 상세의 "찜 수" 표시용. 로그인 여부와 무관하게 조회 가능하다 */
    public long countBookmarks(Long recipeId) {
        String sql = """
                SELECT COUNT(*)
                FROM recipe_bookmarks
                WHERE recipe_id = :recipeId
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("recipeId", recipeId),
                Long.class
        );

        return count == null ? 0 : count;
    }

    public List<StoredRecipeResponse> getBookmarks(
            String userEmail,
            int size
    ) {
        Long userId = requireUserId(userEmail);
        validateSize(size);

        String sql = """
                SELECT
                    r.recipe_id AS recipeId,
                    r.chef_code AS chefCode,
                    r.name AS name,
                    r.summary AS summary,
                    r.cooking_time_min AS cookingTimeMin,
                    r.annoyance_score AS annoyanceScore,
                    r.knife_level AS knifeLevel,
                    r.dish_count AS dishCount,
                    r.image_url AS imageUrl,
                    rb.saved_at AS storedAt
                FROM recipe_bookmarks rb
                JOIN recipes r
                  ON r.recipe_id = rb.recipe_id
                WHERE rb.user_id = :userId
                  AND r.is_active = TRUE
                ORDER BY rb.saved_at DESC, r.recipe_id DESC
                LIMIT :size
                """;

        return queryStoredRecipes(sql, userId, size);
    }

    @Transactional
    public void recordRecentView(String userEmail, Long recipeId) {
        Long userId = requireUserId(userEmail);
        validateActiveRecipe(recipeId);

        String sql = """
                INSERT INTO recent_views (
                    user_id,
                    recipe_id,
                    last_viewed_at
                ) VALUES (
                    :userId,
                    :recipeId,
                    CURRENT_TIMESTAMP
                )
                ON DUPLICATE KEY UPDATE
                    last_viewed_at = CURRENT_TIMESTAMP
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("recipeId", recipeId)
        );
    }

    public List<StoredRecipeResponse> getRecentViews(
            String userEmail,
            int size
    ) {
        Long userId = requireUserId(userEmail);
        validateSize(size);

        String sql = """
                SELECT
                    r.recipe_id AS recipeId,
                    r.chef_code AS chefCode,
                    r.name AS name,
                    r.summary AS summary,
                    r.cooking_time_min AS cookingTimeMin,
                    r.annoyance_score AS annoyanceScore,
                    r.knife_level AS knifeLevel,
                    r.dish_count AS dishCount,
                    r.image_url AS imageUrl,
                    rv.last_viewed_at AS storedAt
                FROM recent_views rv
                JOIN recipes r
                  ON r.recipe_id = rv.recipe_id
                WHERE rv.user_id = :userId
                  AND r.is_active = TRUE
                ORDER BY rv.last_viewed_at DESC, r.recipe_id DESC
                LIMIT :size
                """;

        return queryStoredRecipes(sql, userId, size);
    }

    private List<StoredRecipeResponse> queryStoredRecipes(
            String sql,
            Long userId,
            int size
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("size", size);

        return jdbcTemplate.queryForList(sql, parameters)
                .stream()
                .map(row -> new StoredRecipeResponse(
                        longValue(row.get("recipeId")),
                        stringValue(row.get("chefCode")),
                        chefName(stringValue(row.get("chefCode"))),
                        stringValue(row.get("name")),
                        stringValue(row.get("summary")),
                        integerValue(row.get("cookingTimeMin")),
                        decimalValue(row.get("annoyanceScore")),
                        integerValue(row.get("knifeLevel")),
                        integerValue(row.get("dishCount")),
                        stringValue(row.get("imageUrl")),
                        localDateTimeValue(row.get("storedAt"))
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

    private void validateActiveRecipe(Long recipeId) {
        if (recipeId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "recipeId는 필수입니다."
            );
        }

        String sql = """
                SELECT COUNT(*)
                FROM recipes
                WHERE recipe_id = :recipeId
                  AND is_active = TRUE
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("recipeId", recipeId),
                Long.class
        );

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "활성 레시피를 찾을 수 없습니다."
            );
        }
    }

    private void validateSize(int size) {
        if (size < 1 || size > 50) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "size는 1~50 사이여야 합니다."
            );
        }
    }

    private Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
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

    public record StoredRecipeResponse(
            Long recipeId,
            String chefCode,
            String chefName,
            String name,
            String summary,
            Integer cookingTimeMin,
            BigDecimal annoyanceScore,
            Integer knifeLevel,
            Integer dishCount,
            String imageUrl,
            LocalDateTime storedAt
    ) {
    }
}
