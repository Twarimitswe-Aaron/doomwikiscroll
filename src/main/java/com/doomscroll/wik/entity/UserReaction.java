package com.doomscroll.wik.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "user_reactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "event_id", "reaction_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private HistoricalEvent event;

    @Column(name = "reaction_type", nullable = false, length = 20)
    private String reactionType; // LIKE, LOVE, WOW, SAD, ANGRY, INTERESTING
}
