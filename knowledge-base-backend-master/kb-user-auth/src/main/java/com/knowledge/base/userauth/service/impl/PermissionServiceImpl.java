package com.knowledge.base.userauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.userauth.dto.PermissionDTO;
import com.knowledge.base.userauth.entity.Permission;
import com.knowledge.base.userauth.mapper.PermissionMapper;
import com.knowledge.base.userauth.service.PermissionService;
import com.knowledge.base.userauth.vo.PermissionVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Permission Service implementation class
 *
 * <p>Designed following the Alibaba Java Development Guidelines; implements permission-related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Resource
    private PermissionMapper permissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPermission(PermissionDTO permissionDTO) {
        log.info("Create permission: permissionName={}", permissionDTO.getName());

        // Check whether the permission code already exists
        if (StringUtils.hasText(permissionDTO.getCode())) {
            Permission existPermission = permissionMapper.selectOne(
                    new LambdaQueryWrapper<Permission>()
                            .eq(Permission::getPermissionCode, permissionDTO.getCode())
            );
            if (existPermission != null) {
                throw new BusinessException("Permission code already exists");
            }
        }

        // Check whether the parent permission exists
        if (permissionDTO.getParentId() != null && permissionDTO.getParentId() > 0) {
            Permission parentPermission = permissionMapper.selectById(permissionDTO.getParentId());
            if (parentPermission == null) {
                throw new BusinessException("Parent permission does not exist");
            }
        }

        // Build the permission entity
        Permission permission = new Permission();
        permission.setId(SnowflakeIdGenerator.getInstance().nextId());
        permission.setParentId(permissionDTO.getParentId() != null ? permissionDTO.getParentId() : 0L);
        permission.setPermissionName(permissionDTO.getName());
        permission.setPermissionCode(permissionDTO.getCode());
        permission.setPermissionType(getPermissionTypeValue(permissionDTO.getType()));
        permission.setMenuUrl(permissionDTO.getMenuUrl());
        permission.setApiUrl(permissionDTO.getApiUrl());
        permission.setMethod(permissionDTO.getMethod());
        permission.setSort(permissionDTO.getSortOrder() != null ? permissionDTO.getSortOrder() : 0);
        permission.setStatus(permissionDTO.getStatus() != null ? permissionDTO.getStatus() : 1);

        // Save the permission
        int count = permissionMapper.insert(permission);
        if (count <= 0) {
            throw new BusinessException("Failed to create permission");
        }

        return permission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updatePermission(PermissionDTO permissionDTO) {
        log.info("Update permission: permissionId={}", permissionDTO.getId());

        if (permissionDTO.getId() == null) {
            throw new BusinessException("Permission ID must not be null");
        }

        // Check whether the permission exists
        Permission existPermission = permissionMapper.selectById(permissionDTO.getId());
        if (existPermission == null) {
            throw new BusinessException("Permission does not exist");
        }

        // Check whether the permission code is used by another permission
        if (StringUtils.hasText(permissionDTO.getCode())
                && !permissionDTO.getCode().equals(existPermission.getPermissionCode())) {
            Permission permission = permissionMapper.selectOne(
                    new LambdaQueryWrapper<Permission>()
                            .eq(Permission::getPermissionCode, permissionDTO.getCode())
            );
            if (permission != null && !permission.getId().equals(permissionDTO.getId())) {
                throw new BusinessException("Permission code is already in use");
            }
        }

        // Check whether the parent permission exists
        if (permissionDTO.getParentId() != null && permissionDTO.getParentId() > 0) {
            if (permissionDTO.getParentId().equals(permissionDTO.getId())) {
                throw new BusinessException("The parent permission cannot be itself");
            }
            Permission parentPermission = permissionMapper.selectById(permissionDTO.getParentId());
            if (parentPermission == null) {
                throw new BusinessException("Parent permission does not exist");
            }
        }

        // Build the update entity
        Permission permission = new Permission();
        permission.setId(permissionDTO.getId());
        permission.setParentId(permissionDTO.getParentId() != null ? permissionDTO.getParentId() : 0L);
        permission.setPermissionName(permissionDTO.getName());
        permission.setPermissionCode(permissionDTO.getCode());
        if (StringUtils.hasText(permissionDTO.getType())) {
            permission.setPermissionType(getPermissionTypeValue(permissionDTO.getType()));
        }
        permission.setMenuUrl(permissionDTO.getMenuUrl());
        permission.setApiUrl(permissionDTO.getApiUrl());
        permission.setMethod(permissionDTO.getMethod());
        permission.setSort(permissionDTO.getSortOrder());
        permission.setStatus(permissionDTO.getStatus());

        int count = permissionMapper.updateById(permission);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deletePermission(Long permissionId) {
        log.info("Delete permission: permissionId={}", permissionId);

        if (permissionId == null) {
            throw new BusinessException("Permission ID must not be null");
        }

        // Check whether the permission exists
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException("Permission does not exist");
        }

        // Check for child permissions
        Long childCount = permissionMapper.selectCount(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getParentId, permissionId)
        );
        if (childCount > 0) {
            throw new BusinessException("This permission has child permissions and cannot be deleted");
        }

        // TODO: check whether it is used by any role, and disallow deletion if so

        // Delete the permission
        int count = permissionMapper.deleteById(permissionId);
        return count > 0;
    }

    @Override
    public PermissionVO getPermissionById(Long permissionId) {
        if (permissionId == null) {
            throw new BusinessException("Permission ID must not be null");
        }

        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException("Permission does not exist");
        }

        return convertToVO(permission);
    }

    @Override
    public List<PermissionVO> getPermissionsByParentId(Long parentId) {
        Long targetParentId = parentId != null ? parentId : 0L;
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getParentId, targetParentId)
                        .orderByAsc(Permission::getSort)
        );

        return permissions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<PermissionVO> pagePermissions(Long current, Long size, String keyword) {
        // Build the query conditions
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Permission::getPermissionName, keyword)
                    .or()
                    .like(Permission::getPermissionCode, keyword);
        }

        // Paginated query
        Page<Permission> page = new Page<>(current, size);
        IPage<Permission> permissionPage = permissionMapper.selectPage(page, wrapper);

        // Convert to VO
        return permissionPage.convert(this::convertToVO);
    }

    @Override
    public List<PermissionVO> getPermissionTree() {
        // Query all permissions
        List<Permission> allPermissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .orderByAsc(Permission::getSort)
        );

        // Convert to VO
        List<PermissionVO> permissionVOs = allPermissions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // Build the tree structure
        return buildPermissionTree(permissionVOs, 0L);
    }

    @Override
    public List<PermissionVO> getAllPermissions() {
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getStatus, 1)
                        .orderByAsc(Permission::getSort)
        );

        return permissions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * Convert to VO
     *
     * @param permission permission entity
     * @return permission VO
     */
    private PermissionVO convertToVO(Permission permission) {
        return PermissionVO.builder()
                .id(permission.getId())
                .name(permission.getPermissionName())
                .code(permission.getPermissionCode())
                .type(getPermissionTypeString(permission.getPermissionType()))
                .parentId(permission.getParentId())
                .menuUrl(permission.getMenuUrl())
                .apiUrl(permission.getApiUrl())
                .method(permission.getMethod())
                .sortOrder(permission.getSort())
                .status(permission.getStatus())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }

    /**
     * Build the permission tree
     *
     * @param permissions   permission list
     * @param parentId parent permission ID
     * @return permission tree
     */
    private List<PermissionVO> buildPermissionTree(List<PermissionVO> permissions, Long parentId) {
        List<PermissionVO> tree = new ArrayList<>();

        for (PermissionVO permission : permissions) {
            if (parentId.equals(permission.getParentId())) {
                // Recursively find child permissions
                permission.setChildren(buildPermissionTree(permissions, permission.getId()));
                tree.add(permission);
            }
        }

        return tree;
    }

    /**
     * Get the permission type value
     *
     * @param type permission type string
     * @return permission type value
     */
    private Integer getPermissionTypeValue(String type) {
        if (type == null) {
            return 1;
        }
        return switch (type.trim().toLowerCase()) {
            case "2", "button" -> 2;
            case "3", "api" -> 3;
            case "1", "menu" -> 1;
            default -> 1; // menu
        };
    }

    /**
     * Get the permission type string
     *
     * @param type permission type value
     * @return permission type string
     */
    private String getPermissionTypeString(Integer type) {
        if (type == null) {
            return "menu";
        }
        return switch (type) {
            case 2 -> "button";
            case 3 -> "api";
            default -> "menu";
        };
    }
}
