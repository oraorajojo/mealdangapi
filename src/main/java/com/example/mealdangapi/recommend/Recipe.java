package com.example.mealdangapi.recommend;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "recipes")
@Getter
public class Recipe {

    @Id
    private Long recipeId;

    @Enumerated(EnumType.STRING)
    private ChefCode chefCode;

    private String name;
    private String summary;
    private Integer cookingTimeMin;
    private BigDecimal annoyanceScore;
    private Integer knifeLevel;
    private Integer dishCount;
    private String imageUrl;
    private Boolean isActive;

    protected Recipe() {
    }
}
