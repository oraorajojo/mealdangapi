package com.example.mealdangapi.recommend;

import com.example.mealdangapi.recipe.entity.AnnoyanceBand;
import com.example.mealdangapi.recipe.entity.MealTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommend_logs")
@Getter
@Setter
public class RecommendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendLogId;

    // 비로그인 요청은 NULL로 저장 (회원가입 API 연동 전까지는 항상 NULL)
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String inputIngredientsText;

    // Spring Boot 4(Jackson 3) + Hibernate의 JSON FormatMapper 자동 매핑이
    // 현재 지원되지 않아, JSON 문자열을 직접 만들어 저장한다 (Recommend JsonUtil 참고).
    @Column(columnDefinition = "json")
    private String normalizedIngredientIds;

    // 선택값 — 미세조정을 안 열면 null
    @Enumerated(EnumType.STRING)
    private MealTime mealTime;

    @Enumerated(EnumType.STRING)
    private AnnoyanceBand annoyanceBand;

    @Column(columnDefinition = "json")
    private String requestConditions;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
