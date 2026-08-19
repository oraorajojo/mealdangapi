package com.example.mealdangapi.chef.controller;

import com.example.mealdangapi.chef.service.ChefProfileService;
import com.example.mealdangapi.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me" )
@RequiredArgsConstructor
public class ChefProfileController {

    private final ChefProfileService chefProfileService;

    /**
     * 내 담당 셰프 화면의 기간별 통계 조회.
     * period: week | month | all
     * JWT에서 인증된 이메일만 사용하며, userId는 프론트에서 받지 않는다.
     */
    @GetMapping("/chef-statistics")
    public ResponseEntity<ApiResponse<ChefProfileService.ChefStatisticsResponse>>
    getChefStatistics(
            Authentication authentication,
            @RequestParam(defaultValue = "month") String period
    ) {
        ChefProfileService.ChefStatisticsResponse response =
                chefProfileService.getChefStatistics(
                        authentication.getName(),
                        period
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 내 담당 셰프 화면의 "내가 올린 레시피" 목록 조회.
     * USER_SUBMISSION 여부와 관계없이 JWT 사용자 본인이 author인 레시피만 반환한다.
     */
    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse<List<ChefProfileService.MyRecipeResponse>>>
    getMyRecipes(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ChefProfileService.MyRecipeResponse> response =
                chefProfileService.getMyRecipes(
                        authentication.getName(),
                        size
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
