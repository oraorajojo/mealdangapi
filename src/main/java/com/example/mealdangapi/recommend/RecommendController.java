package com.example.mealdangapi.recommend;

import com.example.mealdangapi.recommend.dto.RecommendRequest;
import com.example.mealdangapi.recommend.dto.RecommendResponse;
import com.example.mealdangapi.recommend.dto.SelectRequest;
import com.example.mealdangapi.recommend.dto.SelectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @PostMapping
    public RecommendResponse recommend(@RequestBody RecommendRequest request) {
        return recommendService.recommend(request);
    }

    @PostMapping("/{recommendLogId}/select")
    public SelectResponse select(@PathVariable Long recommendLogId, @RequestBody SelectRequest request) {
        return recommendService.select(recommendLogId, request);
    }
}
