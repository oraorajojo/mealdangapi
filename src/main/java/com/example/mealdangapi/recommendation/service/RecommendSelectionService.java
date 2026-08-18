package com.example.mealdangapi.recommendation.service;

import com.example.mealdangapi.recommendation.dto.RecommendSelectionRequest;
import com.example.mealdangapi.recommendation.dto.RecommendSelectionResponse;
import com.example.mealdangapi.recommendation.repository.ChefSelectionJdbcRepository;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.entity.UserStatus;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecommendSelectionService {

    private final UserRepository userRepository;
    private final ChefSelectionJdbcRepository chefSelectionJdbcRepository;

    @Transactional
    public RecommendSelectionResponse selectRecipe(
            String userEmail,
            RecommendSelectionRequest request
    ) {
        validateRequest(request);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "인증 사용자를 찾을 수 없습니다."
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "활성 상태의 회원만 최종 메뉴를 선택할 수 있습니다."
            );
        }

        Long recommendLogId = request.getRecommendLogId();
        Long recipeId = request.getRecipeId();

        if (!chefSelectionJdbcRepository.existsRecommendLogForUser(
                recommendLogId,
                user.getUserId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인의 추천 요청만 최종 메뉴로 선택할 수 있습니다."
            );
        }

        if (!chefSelectionJdbcRepository.existsRecommendedRecipe(
                recommendLogId,
                recipeId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이 추천 요청의 후보에 없는 레시피는 선택할 수 없습니다."
            );
        }

        if (chefSelectionJdbcRepository.existsSelectionByRecommendLogId(
                recommendLogId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이 추천 요청은 이미 최종 메뉴를 선택했습니다."
            );
        }

        try {
            Long selectionId = chefSelectionJdbcRepository.save(
                    user.getUserId(),
                    recommendLogId,
                    recipeId
            );

            return new RecommendSelectionResponse(
                    selectionId,
                    recommendLogId,
                    recipeId,
                    "최종 메뉴가 선택되었습니다."
            );
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이 추천 요청은 이미 최종 메뉴를 선택했습니다."
            );
        }
    }

    private void validateRequest(
            RecommendSelectionRequest request
    ) {
        if (request == null
                || request.getRecommendLogId() == null
                || request.getRecipeId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "recommendLogId와 recipeId는 필수입니다."
            );
        }
    }
}
