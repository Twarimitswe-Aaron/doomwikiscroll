package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.CategoryDto;
import com.doomscroll.wik.service.event.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints for retrieving event categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get list of all active categories")
    public ResponseEntity<List<CategoryDto>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed category information by ID")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }
}
