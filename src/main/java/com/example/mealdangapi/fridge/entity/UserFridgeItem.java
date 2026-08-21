package com.example.mealdangapi.fridge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 나만의 냉장고 재료 (USER_FRIDGE_ITEMS) — 담당: 종선
 *
 * 와이어프레임 — "내 냉장고 속 재료를 등록하고 관리"
 *   수량·단위·소비기한을 관리하고 임박 재료를 우선해서 추천에 활용
 *
 * ★ ingredient_id는 FK RESTRICT다.
 *   사용자가 입력한 재료명 문자열을 그대로 저장할 수 없고,
 *   ingredients 테이블에 존재하는 표준 재료 ID여야 한다.
 *
 *   변환은 프론트가 재료 검색 API(GET /api/ingredients/search, 정연 담당)로
 *   먼저 처리한 뒤 ingredientId를 넘겨주는 구조다.
 *   별칭("달걀" → "계란") 처리도 그 API가 담당하므로 여기서는 검증만 한다.
 *
 * ★ UNIQUE 제약이 없다 = 같은 재료를 여러 번 등록할 수 있다.
 *   의도된 설계다. 소비기한이 다른 우유 2팩을 따로 관리해야 하는 경우가 있다.
 */
@Entity
@Table(name = "user_fridge_items")
public class UserFridgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    /**
     * 소유 회원. FK → users.user_id, ON DELETE CASCADE
     *
     * CASCADE인 이유: 탈퇴하면 그 사람의 냉장고 재료는 남길 이유가 없다.
     * (신고 기록이 RESTRICT로 보존되는 것과 대조적 — 운영 이력이 아니므로)
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /**
     * 표준 재료. FK → ingredients.ingredient_id, ON DELETE RESTRICT
     *
     * 수정 가능하다. 사용자가 재료를 잘못 선택했을 때 바꿀 수 있어야 한다.
     */
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    /**
     * 수량. NULL 허용 — "김치 조금"처럼 수량을 모르는 경우가 있다.
     *
     * DECIMAL(10,2)이므로 BigDecimal로 받는다.
     * double로 받으면 0.1 + 0.2 같은 부동소수점 오차가 생긴다.
     *
     * DB에 CHECK (quantity IS NULL OR quantity > 0) 제약이 있어 0 이하는 저장되지 않는다.
     * 서비스에서 먼저 걸러 명확한 에러 코드를 내려준다.
     */
    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;

    /** 단위(개, g, ml, 팩 등). NULL 허용 */
    @Column(name = "unit", length = 30)
    private String unit;

    /**
     * 소비기한. NULL 허용 — 와이어프레임의 "기한 미입력" 상태.
     *
     * LocalDate를 쓰는 이유: 컬럼이 DATE라 시분초가 없다.
     * LocalDateTime으로 매핑하면 ddl-auto=validate에서 타입 불일치로 막힌다.
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** JPA 전용 */
    protected UserFridgeItem() {
    }

    private UserFridgeItem(
            Long userId,
            Long ingredientId,
            BigDecimal quantity,
            String unit,
            LocalDate expiryDate
    ) {
        this.userId = Objects.requireNonNull(userId, "userId는 필수입니다.");
        this.ingredientId = Objects.requireNonNull(ingredientId, "ingredientId는 필수입니다.");
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
    }

    public static UserFridgeItem of(
            Long userId,
            Long ingredientId,
            BigDecimal quantity,
            String unit,
            LocalDate expiryDate
    ) {
        return new UserFridgeItem(userId, ingredientId, quantity, unit, expiryDate);
    }

    /**
     * 재료 정보 수정.
     * userId는 바뀌지 않는다(소유자 이전 개념이 없다).
     */
    public void update(
            Long ingredientId,
            BigDecimal quantity,
            String unit,
            LocalDate expiryDate
    ) {
        this.ingredientId = Objects.requireNonNull(ingredientId, "ingredientId는 필수입니다.");
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
    }

    /** 본인 소유인지 확인 */
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public Long getItemId() {
        return itemId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
