package com.example.mealdangapi.fridge.service;

import com.example.mealdangapi.fridge.dto.request.FridgeItemRequest;
import com.example.mealdangapi.fridge.dto.response.FridgeItemResponse;
import com.example.mealdangapi.fridge.dto.response.FridgeSummaryResponse;
import com.example.mealdangapi.fridge.entity.UserFridgeItem;
import com.example.mealdangapi.fridge.repository.UserFridgeItemRepository;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.recipe.entity.Ingredient;
import com.example.mealdangapi.recipe.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 나만의 냉장고 서비스 (USER_FRIDGE_ITEMS) — 담당: 종선
 *
 * ★ 재료명 → ingredientId 변환은 이 서비스가 하지 않는다.
 *   프론트가 재료 검색 API(정연 담당)로 먼저 확정한 뒤 ingredientId를 보낸다.
 *   여기서는 넘어온 ID가 실제 존재하는지 검증만 한다.
 *
 * ★ 소비기한 임박 기준은 3일이다. (와이어프레임 "3일 이내")
 */
@Service
@RequiredArgsConstructor
public class FridgeService {

    private final UserFridgeItemRepository fridgeItemRepository;
    private final IngredientRepository ingredientRepository;

    /** 임박 판정 기준일. 응답 DTO의 값과 일치해야 한다 */
    private static final int IMMINENT_DAYS = 3;

    /**
     * 내 냉장고 재료 목록 (소비기한 빠른 순).
     */
    @Transactional(readOnly = true)
    public List<FridgeItemResponse> getItems(Long userId) {
        List<UserFridgeItem> items =
                fridgeItemRepository.findAllByUserIdOrderByExpiry(userId);

        return toResponses(items);
    }

    /**
     * 냉장고 요약 집계. 와이어프레임 상단 카드용.
     */
    @Transactional(readOnly = true)
    public FridgeSummaryResponse getSummary(Long userId) {
        LocalDate today = LocalDate.now();

        long total = fridgeItemRepository.countByUserId(userId);
        long expired = fridgeItemRepository
                .countByUserIdAndExpiryDateBefore(userId, today);

        // 오늘 ~ 3일 후까지. BETWEEN은 양끝을 포함한다.
        long imminent = fridgeItemRepository.countByUserIdAndExpiryDateBetween(
                userId, today, today.plusDays(IMMINENT_DAYS)
        );

        long noExpiry = fridgeItemRepository.countByUserIdAndExpiryDateIsNull(userId);

        // 나머지가 여유 있음. 따로 세지 않고 빼서 구하면 쿼리를 하나 아낄 수 있고,
        // 네 값의 합이 total과 반드시 일치하게 된다.
        long safe = total - expired - imminent - noExpiry;

        return new FridgeSummaryResponse(total, expired, imminent, safe, noExpiry);
    }

    /**
     * 소비기한 임박 재료 목록.
     *
     * 와이어프레임 — "임박 재료로 추천 →" 버튼이 이 목록을 추천에 넘긴다.
     * 이미 지난 재료도 포함한다. 오늘 안 쓰면 버려야 하므로 오히려 더 급하다.
     */
    @Transactional(readOnly = true)
    public List<FridgeItemResponse> getExpiringItems(Long userId) {
        LocalDate until = LocalDate.now().plusDays(IMMINENT_DAYS);

        return toResponses(
                fridgeItemRepository.findExpiringItems(userId, until)
        );
    }

    /**
     * 재료 등록.
     *
     * 같은 재료를 여러 번 등록할 수 있다(UNIQUE 제약 없음).
     * 소비기한이 다른 우유 2팩을 따로 관리해야 하는 경우가 있어 의도된 설계다.
     */
    @Transactional
    public FridgeItemResponse addItem(Long userId, FridgeItemRequest request) {
        Ingredient ingredient = findIngredient(request.getIngredientId());

        UserFridgeItem item = fridgeItemRepository.save(
                UserFridgeItem.of(
                        userId,
                        request.getIngredientId(),
                        request.getQuantity(),
                        normalizeUnit(request.getUnit()),
                        request.getExpiryDate()
                )
        );

        return FridgeItemResponse.of(item, ingredient.getName(), LocalDate.now());
    }

    /**
     * 재료 수정. 본인 것만 수정 가능하다.
     */
    @Transactional
    public FridgeItemResponse updateItem(
            Long userId,
            Long itemId,
            FridgeItemRequest request
    ) {
        UserFridgeItem item = findOwnedItem(userId, itemId);
        Ingredient ingredient = findIngredient(request.getIngredientId());

        item.update(
                request.getIngredientId(),
                request.getQuantity(),
                normalizeUnit(request.getUnit()),
                request.getExpiryDate()
        );

        // 변경 감지로 트랜잭션 종료 시 UPDATE가 실행된다. save() 불필요.

        return FridgeItemResponse.of(item, ingredient.getName(), LocalDate.now());
    }

    /**
     * 재료 삭제. 본인 것만 삭제 가능하다.
     *
     * 물리 삭제한다. 냉장고 재료는 운영 이력으로 보존할 이유가 없고,
     * 다 쓴 재료가 계속 남아 있으면 오히려 방해가 된다.
     */
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        UserFridgeItem item = findOwnedItem(userId, itemId);
        fridgeItemRepository.delete(item);
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────

    /**
     * 표준 재료 존재 여부 확인.
     *
     * ingredient_id가 FK RESTRICT라 없는 ID를 넣으면 DB에서 터진다.
     * 먼저 확인해 의미 있는 에러 코드를 내려준다.
     *
     * 규칙사전 §4-3 — 인식하지 못한 재료는 임의로 확정하지 않는다.
     * 프론트가 재료 검색 API를 거치지 않고 임의의 숫자를 보낸 경우가 여기 해당한다.
     */
    private Ingredient findIngredient(Long ingredientId) {
        return ingredientRepository.findById(ingredientId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INGREDIENT_NOT_RECOGNIZED));
    }

    /** 존재하면서 본인 소유인 재료인지 확인 */
    private UserFridgeItem findOwnedItem(Long userId, Long itemId) {
        // findByItemIdAndUserId는 남의 재료를 애초에 조회하지 않는다.
        // 따라서 "없음"과 "남의 것"이 모두 여기서 걸린다.
        //
        // 둘을 구분해 403을 내려주면 "그 itemId는 존재한다"는 정보가 노출되므로
        // 404로 통일하는 편이 안전하다.
        return fridgeItemRepository.findByItemIdAndUserId(itemId, userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.FRIDGE_ITEM_NOT_FOUND));
    }

    /**
     * 재료명을 한 번에 조회해 응답으로 변환.
     *
     * 항목마다 개별 조회하면 12개 목록에 쿼리가 12번 나간다(N+1).
     * findAllById로 묶어 1번으로 처리한다.
     */
    private List<FridgeItemResponse> toResponses(List<UserFridgeItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> ingredientIds = items.stream()
                .map(UserFridgeItem::getIngredientId)
                .distinct()
                .toList();

        Map<Long, String> nameById = new HashMap<>();

        for (Ingredient ingredient : ingredientRepository.findAllById(ingredientIds)) {
            nameById.put(ingredient.getIngredientId(), ingredient.getName());
        }

        LocalDate today = LocalDate.now();

        return items.stream()
                .map(item -> FridgeItemResponse.of(
                        item,
                        nameById.get(item.getIngredientId()),
                        today
                ))
                .toList();
    }

    /** 빈 문자열을 null로 통일한다. ""와 null이 섞이면 화면 처리가 번거로워진다 */
    private String normalizeUnit(String unit) {
        return StringUtils.hasText(unit) ? unit.trim() : null;
    }
}
