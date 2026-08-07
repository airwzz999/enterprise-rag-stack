package com.knowledge.base.userauth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.userauth.dto.RoleDTO;
import com.knowledge.base.userauth.service.RoleService;
import com.knowledge.base.userauth.vo.RoleVO;
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
 * Role management controller
 *
 * <p>Designed following the Alibaba Java Development Guidelines; provides role management endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/roles")
@Tag(name = "Role Management", description = "Role information management endpoints")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class RoleController {

    @Resource
    private RoleService roleService;

    /**
     * Create a role
     *
     * @param roleDTO role information
     * @return role ID
     */
    @PostMapping
    @Operation(summary = "Create role", description = "Create a new role")
    public Result<Long> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        log.info("Create role request: name={}", roleDTO.getName());

        Long roleId = roleService.createRole(roleDTO);
        return Result.success("Role created successfully", roleId);
    }

    /**
     * Update a role
     *
     * @param roleDTO role information
     * @return whether successful
     */
    @PutMapping
    @Operation(summary = "Update role", description = "Update role information")
    public Result<Boolean> updateRole(@Valid @RequestBody RoleDTO roleDTO) {
        log.info("Update role request: roleId={}", roleDTO.getId());

        Boolean success = roleService.updateRole(roleDTO);
        return Result.success("Role updated successfully", success);
    }

    /**
     * Delete a role
     *
     * @param roleId role ID
     * @return whether successful
     */
    @DeleteMapping("/{roleId}")
    @Operation(summary = "Delete role", description = "Delete a role by ID")
    public Result<Boolean> deleteRole(
        @Parameter(description = "Role ID", required = true)
        @PathVariable Long roleId) {
        log.info("Delete role request: roleId={}", roleId);

        Boolean success = roleService.deleteRole(roleId);
        return Result.success("Role deleted successfully", success);
    }

    /**
     * Query a role by ID
     *
     * @param roleId role ID
     * @return role information
     */
    @GetMapping("/{roleId}")
    @Operation(summary = "Query role", description = "Query role details by ID")
    public Result<RoleVO> getRoleById(
        @Parameter(description = "Role ID", required = true)
        @PathVariable Long roleId) {
        log.info("Query role request: roleId={}", roleId);

        RoleVO roleVO = roleService.getRoleById(roleId);
        return Result.success(roleVO);
    }

    /**
     * Paginated role query
     *
     * @param current current page
     * @param size    page size
     * @param keyword search keyword
     * @return paginated role information
     */
    @GetMapping("/page")
    @Operation(summary = "Paginated role query", description = "Query the role list with pagination")
    public Result<IPage<RoleVO>> pageRoles(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword) {
        log.info("Paginated role query request: current={}, size={}, keyword={}", current, size, keyword);

        IPage<RoleVO> page = roleService.pageRoles(current, size, keyword);
        return Result.success(page);
    }

    /**
     * Get all roles
     *
     * @return role list
     */
    @GetMapping("/list")
    @Operation(summary = "Get all roles", description = "Get the full role list")
    public Result<List<RoleVO>> getAllRoles() {
        log.info("Get all roles request");

        List<RoleVO> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    /**
     * Get all roles (simplified, for use in dropdowns etc.)
     *
     * @return role list
     */
    @GetMapping
    @Operation(summary = "Get all roles", description = "Get the full role list (simplified)")
    public Result<List<RoleVO>> listRoles() {
        log.info("Get all roles request (simplified)");

        List<RoleVO> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    /**
     * Assign permissions
     *
     * @param roleId        role ID
     * @param permissionIds permission ID list
     * @return whether successful
     */
    @PostMapping("/{roleId}/permissions")
    @Operation(summary = "Assign permissions", description = "Assign permissions to a role")
    public Result<Boolean> assignPermissions(
        @Parameter(description = "Role ID", required = true)
        @PathVariable Long roleId,
        @RequestBody List<Long> permissionIds) {
        log.info("Assign permissions request: roleId={}, permissionCount={}", roleId, permissionIds.size());

        Boolean success = roleService.assignPermissions(roleId, permissionIds);
        return Result.success("Permissions assigned successfully", success);
    }

    /**
     * Get a role's permissions
     *
     * @param roleId role ID
     * @return permission ID list
     */
    @GetMapping("/{roleId}/permissions")
    @Operation(summary = "Get role permissions", description = "Get the permission list for a role")
    public Result<List<Long>> getRolePermissions(
        @Parameter(description = "Role ID", required = true)
        @PathVariable Long roleId) {
        log.info("Get role permissions request: roleId={}", roleId);

        List<Long> permissionIds = roleService.getRolePermissions(roleId);
        return Result.success(permissionIds);
    }
}
