package com.example.mealdangapi.recommend;

import java.io.Serializable;
import java.util.Objects;

/** recommend_log_results의 복합 PK(recommend_log_id, chef_code)에 대응하는 ID 클래스. */
public class RecommendLogResultId implements Serializable {

    private Long recommendLogId;
    private ChefCode chefCode;

    public RecommendLogResultId() {
    }

    public RecommendLogResultId(Long recommendLogId, ChefCode chefCode) {
        this.recommendLogId = recommendLogId;
        this.chefCode = chefCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecommendLogResultId that)) return false;
        return Objects.equals(recommendLogId, that.recommendLogId) && chefCode == that.chefCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommendLogId, chefCode);
    }
}
