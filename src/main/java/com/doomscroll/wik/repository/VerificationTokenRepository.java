package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenAndTokenType(String token, String tokenType);

    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.used = true WHERE vt.user.id = :userId AND vt.tokenType = :tokenType AND vt.used = false")
    void invalidateTokensByUserAndType(@Param("userId") UUID userId, @Param("tokenType") String tokenType);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
