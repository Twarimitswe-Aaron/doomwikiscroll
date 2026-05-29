package com.doomscroll.wik.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private HistoricalEvent event;

    @Column(name = "progress_percentage")
    @Builder.Default
    private Integer progressPercentage = 0;

    @Column(name = "time_spent_seconds")
    @Builder.Default
    private Integer timeSpentSeconds = 0;

    @Builder.Default
    private Boolean completed = false;

    @Column(name = "last_read_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastReadAt = LocalDateTime.now();
}
