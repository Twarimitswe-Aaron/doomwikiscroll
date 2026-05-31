package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
    Optional<Category> findByName(String name);
}
