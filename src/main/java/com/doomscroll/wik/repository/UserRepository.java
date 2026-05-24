package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailOrUsername(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.email = :email")
    void incrementLoginAttempts(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = 0, u.accountLocked = false WHERE u.email = :email")
    void resetLoginAttempts(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.lastLoginIp = :ip WHERE u.email = :email")
    void updateLastLogin(@Param("email") String email,
                         @Param("lastLoginAt") LocalDateTime lastLoginAt,
                         @Param("ip") String ip);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    void updateUserStatus(@Param("userId") UUID userId, @Param("status") UserStatus status);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.bookmarks WHERE u.id = :userId")
    Optional<User> findByIdWithBookmarks(@Param("userId") UUID userId);
}
