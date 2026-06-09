package com.smartcloset.auth.repository;

import com.smartcloset.auth.domain.AccountActionToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, Long> {

    List<AccountActionToken> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from AccountActionToken token
            join fetch token.user
            where token.tokenHash = :tokenHash
            """)
    Optional<AccountActionToken> findByTokenHashForConsume(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AccountActionToken token where token.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
