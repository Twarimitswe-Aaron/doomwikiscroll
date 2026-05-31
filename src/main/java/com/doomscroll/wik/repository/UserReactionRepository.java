package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.UserReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserReactionRepository extends JpaRepository<UserReaction, UUID> {
    Optional<UserReaction> findByUserIdAndEventId(UUID userId, UUID eventId);
    Optional<UserReaction> findByUserIdAndEventIdAndReactionType(UUID userId, UUID eventId, String reactionType);
}
