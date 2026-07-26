package com.knowledge.base.userauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.userauth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Role Mapper interface
 *
 * <p>Designed following the Alibaba Java Development Guidelines; provides role data access operations</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * Query role codes by user ID
     *
     * @param userId user ID
     * @return role code list, e.g. ["ROLE_USER", "ROLE_REVIEWER"]
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * Query all user IDs that hold the given role code
     *
     * @param roleCode role code, e.g. ROLE_REVIEWER
     * @return user ID list
     */
    List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);

}