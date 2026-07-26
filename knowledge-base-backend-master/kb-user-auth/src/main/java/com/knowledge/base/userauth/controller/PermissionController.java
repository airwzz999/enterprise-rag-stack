package com.knowledge.base.userauth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.userauth.dto.PermissionDTO;
import com.knowledge.base.userauth.service.PermissionService;
import com.knowledge.base.userauth.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Permission management controller
 *
 * <p>Designed following the Alibaba Java Development Guidelines; provides permission management endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/permissions")
@Tag(name = "Permission Management", description = "Permission information management endpoints")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    /**
     * Create a permission
     *
     * @param permissionDTO permission information
     * @return permission ID
     */
    @PostMapping
    @Operation(summary = "Create permission", description = "Create a new permission")
    public Result<Long> createPermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        log.info("Create permission request: code={}", permissionDTO.getCode());

        Long permissionId = permissionService.createPermission(permissionDTO);
        return Result.success("Permission created successfully", permissionId);
    }

    /**
     * Update a permission
     *
     * @param permissionDTO permission information
     * @return whether successful
     */
    @PutMapping
    @Operation(summary = "Update permission", description = "Update permission information")
    public Result<Boolean> updatePermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        log.info("Update permission request: permissionId={}", permissionDTO.getId());

        Boolean success = permissionService.updatePermission(permissionDTO);
        return Result.success("Permission updated successfully", success);
    }

    /**
     * Delete a permission
     *
     * @param permissionId permission ID
     * @return whether successful
     */
    @DeleteMapping("/{permissionId}")
    @Operation(summary = "Delete permission", description = "Delete a permission by ID")
    public Result<Boolean> deletePermission(
        @Parameter(description = "Permission ID", required = true)
        @PathVariable Long permissionId) {
        log.info("Delete permission request: permissionId={}", permissionId);

        Boolean success = permissionService.deletePermission(permissionId);
        return Result.success("Permission deleted successfully", success);
    }

    /**
     * Query a permission by ID
     *
     * @param permissionId permission ID
     * @return permission information
     */
    @GetMapping("/{permissionId}")
    @Operation(summary = "Query permission", description = "Query permission details by ID")
    public Result<PermissionVO> getPermissionById(
        @Parameter(description = "Permission ID", required = true)
        @PathVariable Long permissionId) {
        log.info("Query permission request: permissionId={}", permissionId);

        PermissionVO permissionVO = permissionService.getPermissionById(permissionId);
        return Result.success(permissionVO);
    }

    /**
     * Query direct child permissions by parent permission ID
     *
     * @param permissionId parent permission ID
     * @return child permission list
     */
    @GetMapping("/{permissionId}/children")
    @Operation(summary = "Query child resources", description = "Query direct child permissions by parent permission ID")
    public Result<List<PermissionVO>> getPermissionsByParentId(
        @Parameter(description = "Parent permission ID", required = true)
        @PathVariable Long permissionId) {
        log.info("Query child resources request: permissionId={}", permissionId);

        List<PermissionVO> permissions = permissionService.getPermissionsByParentId(permissionId);
        return Result.success(permissions);
    }

    /**
     * Paginated permission query
     *
     * @param current current page
     * @param size    page size
     * @param keyword search keyword
     * @return paginated permission information
     */
    @GetMapping("/page")
    @Operation(summary = "Paginated permission query", description = "Query the permission list with pagination")
    public Result<IPage<PermissionVO>> pagePermissions(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword) {
        log.info("Paginated permission query request: current={}, size={}, keyword={}", current, size, keyword);

        IPage<PermissionVO> page = permissionService.pagePermissions(current, size, keyword);
        return Result.success(page);
    }

    /**
     * Get the permission tree
     *
     * @return permission tree
     */
    @GetMapping("/tree")
    @Operation(summary = "Get permission tree", description = "Get the full permission tree structure")
    public Result<List<PermissionVO>> getPermissionTree() {
        log.info("Get permission tree request");

        List<PermissionVO> tree = permissionService.getPermissionTree();
        return Result.success(tree);
    }

    /**
     * Get all permissions
     *
     * @return permission list
     */
    @GetMapping("/list")
    @Operation(summary = "Get all permissions", description = "Get the full permission list")
    public Result<List<PermissionVO>> getAllPermissions() {
        log.info("Get all permissions request");

        List<PermissionVO> permissions = permissionService.getAllPermissions();
        return Result.success(permissions);
    }

    /**
     * Get all permissions (simplified, for use in dropdowns etc.)
     *
     * @return permission list
     */
    @GetMapping
    @Operation(summary = "Get all permissions", description = "Get the full permission list (simplified)")
    public Result<List<PermissionVO>> listPermissions() {
        log.info("Get all permissions request (simplified)");

        List<PermissionVO> permissions = permissionService.getAllPermissions();
        return Result.success(permissions);
    }
}
