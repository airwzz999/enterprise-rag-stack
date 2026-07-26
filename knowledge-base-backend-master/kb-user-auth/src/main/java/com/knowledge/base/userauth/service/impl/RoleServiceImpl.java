package com.knowledge.base.userauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.userauth.dto.RoleDTO;
import com.knowledge.base.userauth.entity.Role;
import com.knowledge.base.userauth.mapper.RoleMapper;
import com.knowledge.base.userauth.service.RoleService;
import com.knowledge.base.userauth.vo.RoleVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Role Service implementation class
 *
 * <p>Designed following the Alibaba Java Development Guidelines; implements role-related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleDTO roleDTO) {
        log.info("Create role: roleName={}", roleDTO.getName());

        // Check whether the role name already exists
        Role existRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleName, roleDTO.getName())
        );
        if (existRole != null) {
            throw new BusinessException("Role name already exists");
        }

        // Check whether the role code already exists
        if (StringUtils.hasText(roleDTO.getCode())) {
            existRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>()
                            .eq(Role::getRoleCode, roleDTO.getCode())
            );
            if (existRole != null) {
                throw new BusinessException("Role code already exists");
            }
        }

        // Build the role entity
        Role role = new Role();
        role.setId(SnowflakeIdGenerator.getInstance().nextId());
        role.setRoleName(roleDTO.getName());
        role.setRoleCode(roleDTO.getCode());
        role.setDescription(roleDTO.getDescription());
        role.setStatus(roleDTO.getStatus() != null ? roleDTO.getStatus() : 1);
        role.setSort(0);

        // Save the role
        int count = roleMapper.insert(role);
        if (count <= 0) {
            throw new BusinessException("Failed to create role");
        }

        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRole(RoleDTO roleDTO) {
        log.info("Update role: roleId={}", roleDTO.getId());

        if (roleDTO.getId() == null) {
            throw new BusinessException("Role ID must not be null");
        }

        // Check whether the role exists
        Role existRole = roleMapper.selectById(roleDTO.getId());
        if (existRole == null) {
            throw new BusinessException("Role does not exist");
        }

        // Check whether the role name is used by another role
        if (StringUtils.hasText(roleDTO.getName())
                && !roleDTO.getName().equals(existRole.getRoleName())) {
            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>()
                            .eq(Role::getRoleName, roleDTO.getName())
            );
            if (role != null && !role.getId().equals(roleDTO.getId())) {
                throw new BusinessException("Role name is already in use");
            }
        }

        // Check whether the role code is used by another role
        if (StringUtils.hasText(roleDTO.getCode())
                && !roleDTO.getCode().equals(existRole.getRoleCode())) {
            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>()
                            .eq(Role::getRoleCode, roleDTO.getCode())
            );
            if (role != null && !role.getId().equals(roleDTO.getId())) {
                throw new BusinessException("Role code is already in use");
            }
        }

        // Build the update entity
        Role role = new Role();
        role.setId(roleDTO.getId());
        role.setRoleName(roleDTO.getName());
        role.setRoleCode(roleDTO.getCode());
        role.setDescription(roleDTO.getDescription());
        role.setStatus(roleDTO.getStatus());

        int count = roleMapper.updateById(role);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRole(Long roleId) {
        log.info("Delete role: roleId={}", roleId);

        if (roleId == null) {
            throw new BusinessException("Role ID must not be null");
        }

        // Check whether the role exists
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("Role does not exist");
        }

        long assignedUserCount = countAssignedUsers(roleId);
        if (assignedUserCount > 0) {
            throw new BusinessException("This role is already assigned to users and cannot be deleted");
        }

        // Delete the role-permission associations
        jdbcTemplate.update("DELETE FROM kb_role_permission WHERE role_id = ?", roleId);

        // Also clean up any leftover legacy role associations to avoid stale data
        jdbcTemplate.update("DELETE FROM kb_user_role WHERE role_id = ?", roleId);

        // Delete the role
        int count = roleMapper.deleteById(roleId);
        return count > 0;
    }

    @Override
    public RoleVO getRoleById(Long roleId) {
        if (roleId == null) {
            throw new BusinessException("Role ID must not be null");
        }

        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("Role does not exist");
        }

        return buildRoleVO(role, true);
    }

    @Override
    public IPage<RoleVO> pageRoles(Long current, Long size, String keyword) {
        // Build the query conditions
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Role::getRoleName, keyword)
                    .or()
                    .like(Role::getRoleCode, keyword);
        }

        // Paginated query
        Page<Role> page = new Page<>(current, size);
        IPage<Role> rolePage = roleMapper.selectPage(page, wrapper);

        // Convert to VO
        return rolePage.convert(role -> buildRoleVO(role, true));
    }

    @Override
    public List<RoleVO> getAllRoles() {
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getStatus, 1)
                        .orderByAsc(Role::getSort)
        );

        return roles.stream()
                .map(role -> buildRoleVO(role, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignPermissions(Long roleId, List<Long> permissionIds) {
        log.info("Assign permissions: roleId={}, permissionCount={}", roleId, permissionIds.size());

        if (roleId == null) {
            throw new BusinessException("Role ID must not be null");
        }

        // Check whether the role exists
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("Role does not exist");
        }

        // Delete the existing permission associations
        jdbcTemplate.update("DELETE FROM kb_role_permission WHERE role_id = ?", roleId);

        // Batch insert the permission associations
        if (permissionIds != null && !permissionIds.isEmpty()) {
            String sql = "INSERT INTO kb_role_permission (id, role_id, permission_id, created_at) VALUES (?, ?, ?, NOW())";
            for (Long permissionId : permissionIds) {
                jdbcTemplate.update(sql, SnowflakeIdGenerator.getInstance().nextId(), roleId, permissionId);
            }
        }

        return true;
    }

    @Override
    public List<Long> getRolePermissions(Long roleId) {
        if (roleId == null) {
            throw new BusinessException("Role ID must not be null");
        }

        return jdbcTemplate.queryForList(
                "SELECT permission_id FROM kb_role_permission WHERE role_id = ?",
                Long.class,
                roleId
        );
    }

    private RoleVO buildRoleVO(Role role, boolean includePermissions) {
        List<String> permissions = includePermissions
                ? getRolePermissions(role.getId()).stream().map(String::valueOf).collect(Collectors.toList())
                : List.of();
        Long userCount = countAssignedUsers(role.getId());
        return RoleVO.builder()
                .id(role.getId())
                .name(role.getRoleName())
                .code(role.getRoleCode())
                .description(role.getDescription())
                .status(role.getStatus())
                .permissions(permissions)
                .userCount(userCount != null ? userCount : 0L)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    private Long countAssignedUsers(Long roleId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT ur.user_id) " +
                        "FROM kb_user_role ur " +
                        "INNER JOIN kb_user u ON ur.user_id = u.id " +
                        "WHERE ur.role_id = ? AND u.deleted = 0",
                Long.class,
                roleId
        );
    }
}
