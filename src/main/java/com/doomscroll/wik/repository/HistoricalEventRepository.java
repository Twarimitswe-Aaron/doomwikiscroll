package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.HistoricalEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HistoricalEventRepository extends JpaRepository<HistoricalEvent, UUID> {
    
    @Query("SELECT e FROM HistoricalEvent e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.summary) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<HistoricalEvent> searchEvents(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT e FROM HistoricalEvent e JOIN e.categories c WHERE c.id = :categoryId")
    Page<HistoricalEvent> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("SELECT e FROM HistoricalEvent e WHERE e.status = 'PUBLISHED' AND e.randomKey >= :seed ORDER BY e.randomKey ASC")
    Page<HistoricalEvent> findPublishedEventsAfterRandomKey(@Param("seed") double seed, Pageable pageable);

    @Query("SELECT e FROM HistoricalEvent e WHERE e.status = 'PUBLISHED' AND e.randomKey < :seed ORDER BY e.randomKey ASC")
    Page<HistoricalEvent> findPublishedEventsBeforeRandomKey(@Param("seed") double seed, Pageable pageable);
}
