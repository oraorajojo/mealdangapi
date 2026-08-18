package com.example.mealdangapi.recommendation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
@RequiredArgsConstructor
public class ChefSelectionJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsRecommendLogForUser(
            Long recommendLogId,
            Long userId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM recommend_logs
                WHERE recommend_log_id = ?
                  AND user_id = ?
                """,
                Integer.class,
                recommendLogId,
                userId
        );

        return count != null && count > 0;
    }

    public boolean existsRecommendedRecipe(
            Long recommendLogId,
            Long recipeId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM recommend_log_results
                WHERE recommend_log_id = ?
                  AND recipe_id = ?
                """,
                Integer.class,
                recommendLogId,
                recipeId
        );

        return count != null && count > 0;
    }

    public boolean existsSelectionByRecommendLogId(
            Long recommendLogId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM chef_selections
                WHERE recommend_log_id = ?
                """,
                Integer.class,
                recommendLogId
        );

        return count != null && count > 0;
    }

    public Long save(
            Long userId,
            Long recommendLogId,
            Long recipeId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO chef_selections (
                        user_id,
                        recommend_log_id,
                        recipe_id
                    )
                    VALUES (?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setLong(1, userId);
            statement.setLong(2, recommendLogId);
            statement.setLong(3, recipeId);

            return statement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();

        if (generatedKey == null) {
            throw new IllegalStateException(
                    "최종 메뉴 선택 ID를 생성하지 못했습니다."
            );
        }

        return generatedKey.longValue();
    }
}
