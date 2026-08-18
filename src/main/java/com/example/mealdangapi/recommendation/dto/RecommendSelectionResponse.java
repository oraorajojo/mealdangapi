package com.example.mealdangapi.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendSelectionResponse {

    private Long selectionId;
    private Long recommendLogId;
    private Long recipeId;
    private String message;
}
