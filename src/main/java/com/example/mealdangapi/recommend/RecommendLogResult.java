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
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * id를 직접 세팅(recommendLogId+chefCode, @GeneratedValue 아님)해서 쓰는 복합키 엔티티라,
 * Persistable을 구현 안 하면 save() 할 때마다 Spring Data JPA가 "이미 있는 row인지"
 * 확인하려고 SELECT를 먼저 한 번 날린 뒤에 INSERT한다(merge 경로를 타게 됨).
 * isNew()로 "새로 만든 거면 무조건 새 row"라고 알려주면 그 SELECT 없이 바로 INSERT한다.
 */
@Entity
@Table(name = "recommend_log_results")
@IdClass(RecommendLogResultId.class)
@Getter
@Setter
public class RecommendLogResult implements Persistable<RecommendLogResultId> {

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

    @Override
    public RecommendLogResultId getId() {
        return new RecommendLogResultId(recommendLogId, chefCode);
    }

    /** createdAt은 @PrePersist에서만 채워지므로, 아직 null이면 DB에 없는 새 row라는 뜻이다. */
    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
