package com.example.mealdangapi.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Spring -> FastAPI 재료 인식 사전 1건. 표준명 자기 자신도 term으로 보내고,
 *  별칭은 term=별칭 / canonicalName=표준명으로 별도 항목이 된다. */
public record FastApiIngredientDictionaryEntry(
    String term,
    @JsonProperty("ingredient_id") Long ingredientId,
    @JsonProperty("canonical_name") String canonicalName
) {
}
