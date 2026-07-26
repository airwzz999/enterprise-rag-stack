package com.knowledge.base.userauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.userauth.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * User-role association Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
