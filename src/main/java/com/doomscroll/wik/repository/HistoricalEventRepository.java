package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.HistoricalEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface HistoricalEventRepository extends JpaRepository<HistoricalEvent, UUID> {
    
    @Query("SELECT e FROM HistoricalEvent e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.summary) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<HistoricalEvent> searchEvents(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT e FROM HistoricalEvent e JOIN e.categories c WHERE c.id = :categoryId")
    Page<HistoricalEvent> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query(value = "SELECT * FROM historical_events ORDER BY RANDOM()", nativeQuery = true)
    Page<HistoricalEvent> findRandomEvents(Pageable pageable);
}
