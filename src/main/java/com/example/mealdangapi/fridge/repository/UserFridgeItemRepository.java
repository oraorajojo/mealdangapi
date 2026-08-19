package com.example.mealdangapi.fridge.repository;

import com.example.mealdangapi.fridge.entity.UserFridgeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 냉장고 재료 저장소 (USER_FRIDGE_ITEMS) — 담당: 종선
 */
public interface UserFridgeItemRepository extends JpaRepository<UserFridgeItem, Long> {

    /**
     * 내 냉장고 재료 전체 (소비기한 빠른 순).
     *
     * 와이어프레임 — "등록된 재료 · 소비기한 빠른 순"
     *
     * ★ 정렬에 주의가 필요하다.
     *   expiry_date가 NULL인 항목("기한 미입력")을 그냥 ORDER BY하면
     *   MySQL은 NULL을 가장 작은 값으로 취급해 맨 앞에 놓는다.
     *   기한을 입력하지 않은 재료가 가장 급한 것처럼 보이면 안 되므로
     *   CASE로 NULL을 뒤로 보낸다.
     *
     * 페이징을 쓰지 않는 이유: 개인 냉장고라 수십 개 수준이고,
     * 화면에서도 전체를 한 번에 보여준다.
     */
    @Query("SELECT f FROM UserFridgeItem f "
            + "WHERE f.userId = :userId "
            + "ORDER BY CASE WHEN f.expiryDate IS NULL THEN 1 ELSE 0 END, "
            + "         f.expiryDate ASC, f.itemId ASC")
    List<UserFridgeItem> findAllByUserIdOrderByExpiry(@Param("userId") Long userId);

    /** 소유권 확인과 조회를 한 번에. 남의 재료는 애초에 조회되지 않는다 */
    Optional<UserFridgeItem> findByItemIdAndUserId(Long itemId, Long userId);

    /** 냉장고에 담긴 재료 총 개수 */
    long countByUserId(Long userId);

    /**
     * 소비기한이 지난 재료 수.
     * expiry_date < 오늘
     */
    long countByUserIdAndExpiryDateBefore(Long userId, LocalDate date);

    /**
     * 기간 내 만료 예정 재료 수. (오늘 ~ 기준일)
     * 와이어프레임의 "3일 이내" 집계에 쓴다.
     */
    long countByUserIdAndExpiryDateBetween(Long userId, LocalDate from, LocalDate to);

    /** 소비기한 미입력 재료 수 */
    long countByUserIdAndExpiryDateIsNull(Long userId);

    /**
     * 소비기한 임박 재료 목록.
     *
     * 와이어프레임 — "임박 재료로 추천 →" 버튼이 이 목록을 추천에 넘긴다.
     * 이미 지난 재료도 포함한다(오늘 안 먹으면 버려야 하므로 오히려 더 급하다).
     *
     * 기한 미입력 재료는 제외된다(IS NULL은 BETWEEN에 걸리지 않음).
     */
    @Query("SELECT f FROM UserFridgeItem f "
            + "WHERE f.userId = :userId "
            + "  AND f.expiryDate IS NOT NULL "
            + "  AND f.expiryDate <= :until "
            + "ORDER BY f.expiryDate ASC")
    List<UserFridgeItem> findExpiringItems(
            @Param("userId") Long userId,
            @Param("until") LocalDate until
    );

    /**
     * 같은 재료가 이미 등록되어 있는지.
     *
     * UNIQUE 제약이 없어 중복 등록 자체는 가능하다(소비기한이 다른 우유 2팩 등).
     * 다만 사용자가 실수로 같은 걸 또 넣는 경우가 있어,
     * 등록 시 안내 문구를 띄울 수 있도록 확인 수단을 제공한다.
     */
    boolean existsByUserIdAndIngredientId(Long userId, Long ingredientId);
}
