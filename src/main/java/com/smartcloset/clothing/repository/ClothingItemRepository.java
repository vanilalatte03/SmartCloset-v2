package com.smartcloset.clothing.repository;

import com.smartcloset.clothing.domain.ClothingItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    List<ClothingItem> findByUserIdAndArchivedFalseOrderByIdAsc(Long userId);

    Optional<ClothingItem> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
