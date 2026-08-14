package com.example.mealdangapi.recommend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChefSelectionRepository extends JpaRepository<ChefSelection, Long> {
    boolean existsByRecommendLogId(Long recommendLogId);
}
