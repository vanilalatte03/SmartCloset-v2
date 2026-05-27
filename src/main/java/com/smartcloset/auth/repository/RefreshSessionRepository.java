package com.smartcloset.auth.repository;

import com.smartcloset.auth.domain.RefreshSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    List<RefreshSession> findByUserIdAndRevokedAtIsNull(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshSession session where session.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
