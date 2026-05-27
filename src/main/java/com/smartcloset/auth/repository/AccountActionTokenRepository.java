package com.smartcloset.auth.repository;

import com.smartcloset.auth.domain.AccountActionToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, Long> {

    Optional<AccountActionToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AccountActionToken token where token.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
