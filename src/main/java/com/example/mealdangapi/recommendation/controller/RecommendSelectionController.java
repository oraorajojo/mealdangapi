package com.example.mealdangapi.recommendation.controller;

import com.example.mealdangapi.recommendation.dto.RecommendSelectionRequest;
import com.example.mealdangapi.recommendation.dto.RecommendSelectionResponse;
import com.example.mealdangapi.recommendation.service.RecommendSelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend" )
@RequiredArgsConstructor
public class RecommendSelectionController {

    private final RecommendSelectionService recommendSelectionService;

    @PostMapping("/select")
    public ResponseEntity<RecommendSelectionResponse> selectRecipe(
            Authentication authentication,
            @RequestBody RecommendSelectionRequest request
    ) {
        RecommendSelectionResponse response =
                recommendSelectionService.selectRecipe(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
