package com.example.mealdangapi.recommendation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecommendSelectionRequest {

    private Long recommendLogId;
    private Long recipeId;
}
