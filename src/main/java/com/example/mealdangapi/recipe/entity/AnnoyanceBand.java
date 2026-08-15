package com.example.mealdangapi.recipe.entity;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum AnnoyanceBand {

    LOW(
            new BigDecimal("1.00"),
            new BigDecimal("3.00")
    ),
    MID(
            new BigDecimal("3.00"),
            new BigDecimal("4.00")
    ),
    HIGH(
            new BigDecimal("4.00"),
            new BigDecimal("5.01")
    );

    private final BigDecimal minInclusive;
    private final BigDecimal maxExclusive;

    AnnoyanceBand(
            BigDecimal minInclusive,
            BigDecimal maxExclusive
    ) {
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
    }
}
