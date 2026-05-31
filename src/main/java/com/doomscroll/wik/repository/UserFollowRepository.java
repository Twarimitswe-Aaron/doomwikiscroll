package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {
    Optional<UserFollow> findByUserIdAndCategoryId(UUID userId, UUID categoryId);
    boolean existsByUserIdAndCategoryId(UUID userId, UUID categoryId);
}
