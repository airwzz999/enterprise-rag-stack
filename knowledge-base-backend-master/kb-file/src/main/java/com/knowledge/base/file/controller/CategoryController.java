package com.knowledge.base.file.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.file.dto.CategoryDTO;
import com.knowledge.base.file.service.CategoryService;
import com.knowledge.base.file.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * File category controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/file/categories")
@RequiredArgsConstructor
@Tag(name = "File Category Management", description = "CRUD endpoints for file categories")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Create a category
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create category", description = "Create a new file category")
    public Result<CategoryVO> create(@Valid @RequestBody CategoryDTO dto) {
        log.info("Create category request: {}", dto.getName());
        CategoryVO result = categoryService.create(dto);
        return Result.success(result);
    }

    /**
     * Update a category
     */
    @PutMapping
    @Operation(summary = "Update category", description = "Update the information of the specified category")
    public Result<CategoryVO> update(@Valid @RequestBody CategoryDTO dto) {
        log.info("Update category request: id={}", dto.getId());
        CategoryVO result = categoryService.update(dto);
        return Result.success(result);
    }

    /**
     * Delete a category
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category", description = "Delete the specified category (child categories must be deleted first)")
    public Result<Void> delete(@Parameter(description = "Category ID") @PathVariable Long id) {
        log.info("Delete category request: id={}", id);
        categoryService.delete(id);
        return Result.success();
    }

    /**
     * Get category details by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category details", description = "Get the detailed information of a category by ID")
    public Result<CategoryVO> getById(@Parameter(description = "Category ID") @PathVariable Long id) {
        CategoryVO result = categoryService.getById(id);
        return Result.success(result);
    }

    /**
     * Get all categories (flat list)
     */
    @GetMapping
    @Operation(summary = "Get all categories", description = "Get a flat list of all categories")
    public Result<List<CategoryVO>> listAll() {
        List<CategoryVO> result = categoryService.listAll();
        return Result.success(result);
    }

    /**
     * Get the category tree structure
     */
    @GetMapping("/tree")
    @Operation(summary = "Get category tree", description = "Get the tree structure of categories")
    public Result<List<CategoryVO>> getTree() {
        List<CategoryVO> result = categoryService.getTree();
        return Result.success(result);
    }

    /**
     * Get the list of child categories by parent category ID
     */
    @GetMapping("/children/{parentId}")
    @Operation(summary = "Get child categories", description = "Get the list of child categories by parent category ID")
    public Result<List<CategoryVO>> listByParentId(
            @Parameter(description = "Parent category ID, 0 indicates a top-level category") @PathVariable Long parentId) {
        List<CategoryVO> result = categoryService.listByParentId(parentId);
        return Result.success(result);
    }

    /**
     * Enable a category
     */
    @PutMapping("/{id}/enable")
    @Operation(summary = "Enable category", description = "Set the specified category to enabled status")
    public Result<CategoryVO> enable(@Parameter(description = "Category ID") @PathVariable Long id) {
        log.info("Enable category request: id={}", id);
        CategoryVO result = categoryService.updateStatus(id, 1);
        return Result.success(result);
    }

    /**
     * Disable a category
     */
    @PutMapping("/{id}/disable")
    @Operation(summary = "Disable category", description = "Set the specified category to disabled status")
    public Result<CategoryVO> disable(@Parameter(description = "Category ID") @PathVariable Long id) {
        log.info("Disable category request: id={}", id);
        CategoryVO result = categoryService.updateStatus(id, 0);
        return Result.success(result);
    }
}
