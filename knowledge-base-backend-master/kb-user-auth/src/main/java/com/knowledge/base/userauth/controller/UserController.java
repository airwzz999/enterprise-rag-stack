package com.knowledge.base.userauth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.userauth.dto.UserDTO;
import com.knowledge.base.userauth.service.UserService;
import com.knowledge.base.userauth.vo.UserStatisticsVO;
import com.knowledge.base.userauth.vo.UserVO;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * User controller
 *
 * <p>Designed following the Alibaba Java Development Guidelines; provides user management endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "User information management endpoints")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * Create a user
     *
     * @param userDTO user information
     * @return user ID
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user")
    public Result<Long> createUser(@Valid @RequestBody UserDTO userDTO) {
        log.info("Create user request: username={}", userDTO.getUsername());

        Long userId = userService.createUser(userDTO);
        return Result.success("User created successfully", userId);
    }

    /**
     * Update a user
     *
     * @param userDTO user information
     * @return whether successful
     */
    @PutMapping
    @Operation(summary = "Update user", description = "Update user information")
    public Result<Boolean> updateUser(@Valid @RequestBody UserDTO userDTO) {
        log.info("Update user request: userId={}", userDTO.getId());

        Boolean success = userService.updateUser(userDTO);
        return Result.success("User updated successfully", success);
    }

    /**
     * Delete a user
     *
     * @param userId user ID
     * @return whether successful
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Delete a user by ID")
    public Result<Boolean> deleteUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long userId) {
        log.info("Delete user request: userId={}", userId);

        Boolean success = userService.deleteUser(userId);
        return Result.success("User deleted successfully", success);
    }

    /**
     * Query a user by ID
     *
     * @param userId user ID
     * @return user information
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Query user", description = "Query user information by ID")
    public Result<UserVO> getUserById(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long userId) {
        log.info("Query user request: userId={}", userId);

        UserVO userVO = userService.getUserById(userId);
        return Result.success(userVO);
    }

    /**
     * Paginated user query
     *
     * @param current current page
     * @param size    page size
     * @param keyword search keyword
     * @param role    role filter
     * @param status  status filter
     * @return paginated user information
     */
    @GetMapping("/page")
    @Operation(summary = "Paginated user query", description = "Query the user list with pagination")
    public Result<IPage<UserVO>> pageUsers(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
        @Parameter(description = "Role filter") @RequestParam(required = false) String role,
        @Parameter(description = "Status filter") @RequestParam(required = false) Integer status) {
        log.info("Paginated user query request: current={}, size={}, keyword={}, role={}, status={}", current, size, keyword, role, status);

        IPage<UserVO> page = userService.pageUsers(current, size, keyword, role, status);
        return Result.success(page);
    }

    /**
     * Reset a user's password
     *
     * @param userId      user ID
     * @param newPassword new password
     * @return whether successful
     */
    @PutMapping("/{userId}/password/reset")
    @Operation(summary = "Reset password", description = "Administrator resets a user's password")
    public Result<Boolean> resetPassword(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long userId,
        @Parameter(description = "New password", required = true)
        @RequestParam String newPassword) {
        log.info("Reset user password request: userId={}", userId);

        Boolean success = userService.resetPassword(userId, newPassword);
        return Result.success("Password reset successfully", success);
    }

    /**
     * Change the current user's password
     *
     * @param oldPassword old password
     * @param newPassword new password
     * @return whether successful
     */
    @PutMapping("/password/change")
    @Operation(summary = "Change password", description = "User changes their own password")
    public Result<Boolean> changePassword(
        @Parameter(description = "Old password", required = true)
        @RequestParam String oldPassword,
        @Parameter(description = "New password", required = true)
        @RequestParam String newPassword) {
        log.info("Change password request");

        Boolean success = userService.changePassword(oldPassword, newPassword);
        return Result.success("Password changed successfully", success);
    }

    /**
     * Assign roles to a user
     *
     * @param userId  user ID
     * @param roleIds role ID list
     * @return whether successful
     */
    @PostMapping("/{userId}/roles")
    @Operation(summary = "Assign roles", description = "Assign roles to a user")
    public Result<Boolean> assignRoles(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long userId,
        @Parameter(description = "Role ID list", required = true)
        @RequestBody List<Long> roleIds) {
        log.info("Assign roles request: userId={}, roleIds={}", userId, roleIds);

        Boolean success = userService.assignRoles(userId, roleIds);
        return Result.success("Roles assigned successfully", success);
    }

    /**
     * Get a user's assigned roles
     *
     * @param userId user ID
     * @return role ID list
     */
    @GetMapping("/{userId}/roles")
    @Operation(summary = "Get user roles", description = "Get the roles assigned to a user")
    public Result<List<Long>> getUserRoles(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long userId) {
        log.info("Get user roles request: userId={}", userId);

        List<Long> roleIds = userService.getUserRoles(userId);
        return Result.success(roleIds);
    }

    /**
     * Assign permissions to a user
     *
     * @param userId        user ID
     * @param permissionIds permission ID list
     * @return whether successful
     */
    @PostMapping("/{userId}/permissions")
    @Operation(summary = "Assign permissions", description = "Assign permissions directly to a user (not via a role)")
    public Result<Boolean> assignPermissions(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long userId,
        @Parameter(description = "Permission ID list", required = true)
        @RequestBody List<Long> permissionIds) {
        log.info("Assign permissions request: userId={}, permissionCount={}", userId, permissionIds.size());

        Boolean success = userService.assignPermissions(userId, permissionIds);
        return Result.success("Permissions assigned successfully", success);
    }

    /**
     * Get all of a user's permissions
     *
     * @param userId user ID
     * @return permission code list
     */
    @GetMapping("/{userId}/permissions")
    @Operation(summary = "Get user permissions", description = "Get all of a user's permissions (including role-based and directly assigned permissions)")
    public Result<List<String>> getUserPermissions(
        @Parameter(description = "User ID", required = true)
        @PathVariable Long userId) {
        log.info("Get user permissions request: userId={}", userId);

        List<String> permissions = userService.getUserPermissions(userId);
        return Result.success(permissions);
    }

    /**
     * Get the current logged-in user's statistics (document count, views, likes, comments)
     *
     * @return user statistics
     */
    @GetMapping("/me/stats")
    @Operation(summary = "Get current user statistics", description = "Get the current logged-in user's document count, views, likes, and comments")
    public Result<UserStatisticsVO> getMyStatistics() {
        Long userId = UserContextUtil.getUserId();
        log.info("Get current user statistics: userId={}", userId);

        UserStatisticsVO stats = userService.getUserStatistics(userId);
        return Result.success(stats);
    }
}
