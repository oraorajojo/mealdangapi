package com.example.mealdangapi.recommend;

import com.example.mealdangapi.recipe.entity.ChefCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommend_log_results")
@IdClass(RecommendLogResultId.class)
@Getter
@Setter
public class RecommendLogResult {

    @Id
    private Long recommendLogId;

    @Id
    @Enumerated(EnumType.STRING)
    private ChefCode chefCode;

    private Long recipeId;

    @Column(name = "recommendation_rank", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer recommendationRank;

    private BigDecimal matchScore;
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
