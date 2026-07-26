package com.knowledge.base.userauth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.userauth.dto.RoleDTO;
import com.knowledge.base.userauth.vo.RoleVO;

import java.util.List;

/**
 * Role Service interface
 *
 * <p>Provides role-related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface RoleService {

    /**
     * Create a role
     *
     * @param roleDTO role information
     * @return role ID
     */
    Long createRole(RoleDTO roleDTO);

    /**
     * Update a role
     *
     * @param roleDTO role information
     * @return whether successful
     */
    Boolean updateRole(RoleDTO roleDTO);

    /**
     * Delete a role
     *
     * @param roleId role ID
     * @return whether successful
     */
    Boolean deleteRole(Long roleId);

    /**
     * Query a role by ID
     *
     * @param roleId role ID
     * @return role information
     */
    RoleVO getRoleById(Long roleId);

    /**
     * Paginated role query
     *
     * @param current current page
     * @param size    page size
     * @param keyword search keyword
     * @return paginated role information
     */
    IPage<RoleVO> pageRoles(Long current, Long size, String keyword);

    /**
     * Get all roles
     *
     * @return role list
     */
    List<RoleVO> getAllRoles();

    /**
     * Assign permissions
     *
     * @param roleId        role ID
     * @param permissionIds permission ID list
     * @return whether successful
     */
    Boolean assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * Get a role's permissions
     *
     * @param roleId role ID
     * @return permission ID list
     */
    List<Long> getRolePermissions(Long roleId);
}
