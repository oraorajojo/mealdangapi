package com.example.mealdangapi.recommend;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chef_selections")
@Getter
@Setter
public class ChefSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long selectionId;

    // 현재 DB 스키마상 NOT NULL. 로그인 연동 전까지는 요청 본문으로 임시 전달받는다 (TODO: 인증 붙으면 SecurityContext에서 추출).
    private Long userId;

    private Long recommendLogId;
    private Long recipeId;
    private LocalDateTime selectedAt;

    @PrePersist
    void prePersist() {
        if (selectedAt == null) {
            selectedAt = LocalDateTime.now();
        }
    }
}
