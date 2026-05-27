package com.smartcloset.auth.repository;

import com.smartcloset.auth.domain.RefreshSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    List<RefreshSession> findByUserIdAndRevokedAtIsNull(Long userId);
}
