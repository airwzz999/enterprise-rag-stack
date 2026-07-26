package com.knowledge.base.document.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.CategoryDTO;
import com.knowledge.base.document.service.CategoryService;
import com.knowledge.base.document.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Category management Controller
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, provides document category management endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/categories")
@Tag(name = "Category Management", description = "Document category management endpoints")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * Creates a category
     *
     * @param categoryDTO category information
     * @return category ID
     */
    @PostMapping
    @Operation(summary = "Create category", description = "Creates a new category")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CATEGORY)")
    public Result<Long> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Create category request: name={}", categoryDTO.getName());

        Long categoryId = categoryService.createCategory(categoryDTO);
        return Result.success("Category created successfully", categoryId);
    }

    /**
     * Updates a category
     *
     * @param categoryDTO category information
     * @return whether successful
     */
    @PutMapping
    @Operation(summary = "Update category", description = "Updates category information")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CATEGORY)")
    public Result<Boolean> updateCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Update category request: categoryId={}", categoryDTO.getId());

        Boolean success = categoryService.updateCategory(categoryDTO);
        return Result.success("Category updated successfully", success);
    }

    /**
     * Deletes a category
     *
     * @param categoryId category ID
     * @return whether successful
     */
    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Delete category", description = "Deletes a category by ID")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CATEGORY)")
    public Result<Boolean> deleteCategory(
        @Parameter(description = "Category ID", required = true)
        @PathVariable Long categoryId) {
        log.info("Delete category request: categoryId={}", categoryId);

        Boolean success = categoryService.deleteCategory(categoryId);
        return Result.success("Category deleted successfully", success);
    }

    /**
     * Queries a category by ID
     *
     * @param categoryId category ID
     * @return category information
     */
    @GetMapping("/{categoryId}")
    @Operation(summary = "Query category", description = "Queries category details by category ID")
    public Result<CategoryVO> getCategoryById(
        @Parameter(description = "Category ID", required = true)
        @PathVariable Long categoryId) {
        log.info("Query category request: categoryId={}", categoryId);

        CategoryVO categoryVO = categoryService.getCategoryById(categoryId);
        return Result.success(categoryVO);
    }

    /**
     * Gets the category tree
     *
     * @return category tree
     */
    @GetMapping("/tree")
    @Operation(summary = "Get category tree", description = "Gets the full category tree structure")
    public Result<List<CategoryVO>> getCategoryTree() {
        log.info("Get category tree request");

        List<CategoryVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * Gets subcategories
     *
     * @param parentId parent category ID
     * @return subcategory list
     */
    @GetMapping("/children/{parentId}")
    @Operation(summary = "Get subcategories", description = "Gets the subcategory list for the specified parent category")
    public Result<List<CategoryVO>> getChildren(
        @Parameter(description = "Parent category ID", required = true)
        @PathVariable Long parentId) {
        log.info("Get subcategories request: parentId={}", parentId);

        List<CategoryVO> children = categoryService.getChildren(parentId);
        return Result.success(children);
    }

    /**
     * Moves a category
     *
     * @param categoryId    category ID
     * @param newParentId new parent category ID
     * @return whether successful
     */
    @PutMapping("/{categoryId}/move")
    @Operation(summary = "Move category", description = "Moves a category to a new parent category")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CATEGORY)")
    public Result<Boolean> moveCategory(
        @Parameter(description = "Category ID", required = true)
        @PathVariable Long categoryId,
        @Parameter(description = "New parent category ID", required = true)
        @RequestParam Long newParentId) {
        log.info("Move category request: categoryId={}, newParentId={}", categoryId, newParentId);

        Boolean success = categoryService.moveCategory(categoryId, newParentId);
        return Result.success("Category moved successfully", success);
    }

    /**
     * Gets all categories (flat)
     *
     * @return category list
     */
    @GetMapping("/list")
    @Operation(summary = "Get all categories", description = "Gets the full category list (flat)")
    public Result<List<CategoryVO>> getAllCategories() {
        log.info("Get all categories request");

        List<CategoryVO> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }
}
