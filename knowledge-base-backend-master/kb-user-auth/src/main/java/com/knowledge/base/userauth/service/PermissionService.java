package com.knowledge.base.userauth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.userauth.dto.PermissionDTO;
import com.knowledge.base.userauth.vo.PermissionVO;

import java.util.List;

/**
 * Permission Service interface
 *
 * <p>Provides permission-related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface PermissionService {

    /**
     * Create a permission
     *
     * @param permissionDTO permission information
     * @return permission ID
     */
    Long createPermission(PermissionDTO permissionDTO);

    /**
     * Update a permission
     *
     * @param permissionDTO permission information
     * @return whether successful
     */
    Boolean updatePermission(PermissionDTO permissionDTO);

    /**
     * Delete a permission
     *
     * @param permissionId permission ID
     * @return whether successful
     */
    Boolean deletePermission(Long permissionId);

    /**
     * Query a permission by ID
     *
     * @param permissionId permission ID
     * @return permission information
     */
    PermissionVO getPermissionById(Long permissionId);

    /**
     * Query direct child permissions by parent permission ID
     *
     * @param parentId parent permission ID
     * @return child permission list
     */
    List<PermissionVO> getPermissionsByParentId(Long parentId);

    /**
     * Paginated permission query
     *
     * @param current current page
     * @param size    page size
     * @param keyword search keyword
     * @return paginated permission information
     */
    IPage<PermissionVO> pagePermissions(Long current, Long size, String keyword);

    /**
     * Get the permission tree
     *
     * @return permission tree
     */
    List<PermissionVO> getPermissionTree();

    /**
     * Get all permissions
     *
     * @return permission list
     */
    List<PermissionVO> getAllPermissions();
}
