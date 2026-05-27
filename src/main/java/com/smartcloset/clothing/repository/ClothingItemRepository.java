package com.smartcloset.clothing.repository;

import com.smartcloset.clothing.domain.ClothingItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    List<ClothingItem> findByUserIdAndArchivedFalseOrderByIdAsc(Long userId);

    Optional<ClothingItem> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("""
            select item.imageStoredFilename
            from ClothingItem item
            where item.user.id = :userId
              and item.imageStoredFilename is not null
            """)
    List<String> findImageStoredFilenamesByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ClothingItem item where item.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
